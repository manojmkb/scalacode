package gov.hhs.cms.dsrs.validators

import gov.hhs.cms.arch.validation.{ValidationError, ValidationResult, Validator}
import gov.hhs.cms.dsrs.validators.FieldRulesValidator.CfgCtx
import gov.hhs.cms.arch.exception.ConfigurationException
import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import java.time.Instant

import gov.hhs.cms.dsrs.DSRSConstants._

import scala.collection.JavaConverters._
import org.json4s._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

/**
  * Contains field-level validation rules. Reads from reference data to validate individual input fields.
  * @author Megan Bishop, Taylor Mattison
  */
class FieldRulesValidator(cfgCtx : CfgCtx) extends ValidatorCore with Validator[(JObject, String)] {

  private val refData = cfgCtx.refData.getRefData
  private val LOG: Logger = LoggerFactory.getLogger(classOf[FieldRulesValidator])

  def apply(input: (JObject, String)): ValidationResult = {

    val (json, documentCategory) = input
    val result = new ValidationResult()
    val nodes = json.extract[Map[String, JValue]]
    nodes.foreach(node => {
      val (key, _) = node

      val context: Option[String] = {
        if (refData.isValidPath(COMMON_CONFIG_NAME, Instant.now(), FIELD_RULES_CONFIG, key)) {
          Option(COMMON_CONFIG_NAME)
        }
        else if (refData.isValidPath(documentCategory, Instant.now(), FIELD_RULES_CONFIG, key)) {
          Option(documentCategory)
        }
        else {
          result.add(new ValidationError(key, getErrorCodeFromRefData(refData, "unrecognizedField")))
          Option.empty
        }
      }

      result.merge(
        validateDataType.and(validateCheckAllowedValues).and(validateCheckValidationRules).and(validateCheckNumericValue)
        (node, context)
      )
    })
    result
  }

  /**
    * Checks the data type of the input value
    * @return validator
    */
  def validateDataType: Validator[(JField, Option[String])] = {
    new Validator[(JField, Option[String])] {
      override def apply(input: (JField, Option[String])): ValidationResult = {
        val ((inputKey, inputValue), context) = input
        val result = new ValidationResult()

        if (context.isDefined && refData.isValidPath(context.get, Instant.now(), FIELD_RULES_CONFIG,
          inputKey, DATA_TYPE_CONFIG)) {
          val expectedDataType = refData.getString(context.get, Instant.now(), FIELD_RULES_CONFIG,
            inputKey, DATA_TYPE_CONFIG)
            val inputDataType = getDataTypeString(inputValue)

            if (inputDataType != expectedDataType || inputDataType == "Unrecognized") {
              result.add(new ValidationError(inputKey, getErrorCodeFromRefData(refData, "invalidDataType")))
            }
          }
          result
        }
      }
    }

  /**
    * Checks that the value is one of the allowed values
    * @return validator
    */
  def validateCheckAllowedValues: Validator[(JField, Option[String])] = {
    new Validator[(JField, Option[String])] {
      override def apply(input: (JField, Option[String])): ValidationResult = {
        val ((inputKey, inputJValue), context) = input
        val result = new ValidationResult()

        if (context.isDefined && refData.isValidPath(context.get, Instant.now(), FIELD_RULES_CONFIG,
          inputKey, ALLOWED_VALUES_CONFIG)) {
          val allowedValues = refData.getStringList(context.get, Instant.now(), FIELD_RULES_CONFIG,
            inputKey, ALLOWED_VALUES_CONFIG)
          val inputStringValue = inputJValue.extract[String]
          if (!allowedValues.contains(inputStringValue)) {
            result.add(new ValidationError(inputKey, getErrorCodeFromRefData(refData, "unrecognizedReferenceType")))
          }
        }
        result
      }
    }
  }

  /**
    * Checks the input against all of its validation rules (regexes)
    * @return validator
    */
  def validateCheckValidationRules: Validator[(JField, Option[String])] = {
    new Validator[(JField, Option[String])] {
      override def apply(input: (JField, Option[String])): ValidationResult = {
        val ((inputKey, inputJValue), context) = input
        val result = new ValidationResult()

        if (context.isDefined && refData.isValidPath(context.get, Instant.now(), FIELD_RULES_CONFIG,
          inputKey, VALIDATION_RULES_CONFIG)) {
          val validationRules = refData.getStringList(context.get, Instant.now(), FIELD_RULES_CONFIG,
            inputKey, VALIDATION_RULES_CONFIG).asScala
          val inputStringValue = inputJValue.extract[String]
          validationRules.map(rule => splitValidationRule(rule))
            .filterNot(rule => passesValidationRule(inputStringValue, rule))
            .foreach(failedRule => result.add(new ValidationError(inputKey, lookupErrorCode(failedRule._1))))
        }
        result
      }
    }
  }

  /**
  * Checks the input against all of its numeric rules
  * @return validator
  */
  def validateCheckNumericValue: Validator[(JField, Option[String])] = {
    new Validator[(JField, Option[String])] {
      override def apply(input: (JField, Option[String])): ValidationResult = {
        val ((inputKey, inputJValue), context) = input
        val result = new ValidationResult()

        if (context.isDefined && refData.isValidPath(context.get, Instant.now(), FIELD_RULES_CONFIG,
          inputKey, NUMERIC_RULES_CONFIG)) {
          val minAndMax = refData.getIntList(context.get, Instant.now(), FIELD_RULES_CONFIG,
            inputKey, NUMERIC_RULES_CONFIG)
          val inputStringValue = Try(inputJValue.extract[Int])
          if (inputStringValue.isSuccess) {
            if (inputStringValue.get <= minAndMax.get(0)) {
              result.add(new ValidationError(inputKey, getErrorCodeFromRefData(refData, "minimumFileSize")))
            }
            else if (inputStringValue.get >= minAndMax.get(1)) {
              result.add(new ValidationError(inputKey, getErrorCodeFromRefData(refData, "maximumFileSize")))
            }
          }
        }
        result
      }
    }
  }

  /**
    * Splits the regex on '=='
    *
    * @param validationRule the regex that needs to be split
    * @return the name of the validation rule and its parameter
    */
  private def splitValidationRule(validationRule: String): (String, Option[String]) = {
    validationRule.split("==") match {
      case Array(name, param) => (name, Option(param))
      case Array(name) => (name, Option.empty)
      case _ => throw new ConfigurationException("Validation rule '" + validationRule + "' failed to parse")
    }
  }

  /**
    * Matches the input value and the regex
    *
    * @param value the input value
    * @param validationRule the regex
    * @return the boolean result
    */
  private def passesValidationRule(value: String, validationRule: (String, Option[String])): Boolean = {
    val (ruleName, ruleParam) = validationRule
    val regex =
      if (ruleParam.isDefined) {
        refData.getString(VALIDATION_RULES_CONFIG_NAME, Instant.now(), ruleName, "regex").replaceAll("%arg%", ruleParam.get)
      } else {
        refData.getString(VALIDATION_RULES_CONFIG_NAME, Instant.now(), ruleName, "regex")
      }
    value.matches(regex)
  }

  /**
    * Looks up the the error code for the validation rule (regex)
    *
    * @param validationRule the name of the regex being applied
    * @return the error code
    */
  private def lookupErrorCode(validationRule: String): String = {
    val errorMessageReference = refData.getString(VALIDATION_RULES_CONFIG_NAME, Instant.now(), validationRule, "errorMessageReference")
    if (!refData.isValidPath(VALIDATION_ERROR_CONFIG_NAME, Instant.now(), "errors", errorMessageReference, "code")) {
      throw new ConfigurationException("Could not find " + errorMessageReference + " error code in configuration")
    }
    getErrorCodeFromRefData(refData, errorMessageReference)
  }

  private def getDataTypeString(inputValue: JValue): String = {
    inputValue match {
      case _: JArray => "Array"
      case _: JBool => "Boolean"
      case _: JDouble => "Number"
      case _: JInt => "Number"
      case _: JObject => "Object"
      case _: JString => "String"
      case default => "Unrecognized"
    }
  }
}

/**
  * Contains configuration for the FieldRulesValidator
  * refData - Able to access reference data such as validation configuration
  */
object FieldRulesValidator {
  case class CfgCtx(refData: RefDataAccessorsCore)
}