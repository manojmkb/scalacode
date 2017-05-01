package gov.hhs.cms.dsrs.validators

import gov.hhs.cms.arch.validation.{ValidationError, ValidationResult, Validator}
import gov.hhs.cms.dsrs.DSRSConstants._
import gov.hhs.cms.dsrs.validators.DocumentRulesValidator.CfgCtx
import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import org.json4s.JObject
import org.json4s.JsonAST.JValue

/**
  * Contains document-level validation rules. Reads from reference data to validate groups of input fields.
  * @author Megan Bishop, Taylor Mattison
  */
class DocumentRulesValidator(cfgCtx : CfgCtx) extends ValidatorCore with Validator[(JObject, String)] {

  private val refData = cfgCtx.refData.getRefData

  def apply(input: (JObject, String)): ValidationResult = {

    val (json, documentCategory) = input
    val inputFields: Set[String] = json.extract[Map[String, JValue]].keySet

    /**
      * Checks if all required fields are present
      * @return validator
      */
    def validateRequiredFieldsToStore: Validator[Set[String]] = {
      new Validator[Set[String]] {
        override def apply(inputKeys: Set[String]): ValidationResult = {
          val requiredKeys: Set[String] = getStringListIfExists(refData, COMMON_CONFIG_NAME, REQUIRED_TO_STORE_CONFIG_RULE) ++
            getStringListIfExists(refData, documentCategory, REQUIRED_TO_STORE_CONFIG_RULE)
          val result = new ValidationResult()
          requiredKeys.diff(inputKeys)
            .foreach(missingKey => result.add(new ValidationError(missingKey,
              getErrorCodeFromRefData(refData, REQUIRED_TO_STORE_CONFIG_RULE))))
          result
        }
      }
    }

    /**
      * Checks if there is at least one identifier
      * @return validator
      */
    def validateAtLeastOneIdentifierToStore: Validator[Set[String]] = {
      new Validator[Set[String]] {
        override def apply(inputKeys: Set[String]): ValidationResult = {
          val possibleKeys: Set[String] = getStringListIfExists(refData, COMMON_CONFIG_NAME,
            AT_LEAST_ONE_IDENTIFIER_TO_STORE_CONFIG_RULE) ++
            getStringListIfExists(refData, documentCategory, AT_LEAST_ONE_IDENTIFIER_TO_STORE_CONFIG_RULE)
          val result = new ValidationResult()
          if (!inputKeys.exists(possibleKeys.contains) && possibleKeys.nonEmpty) {
            result.add(new ValidationError("documentStoreInput", getErrorCodeFromRefData(refData,
              AT_LEAST_ONE_IDENTIFIER_TO_STORE_CONFIG_RULE)))
          }
          result
        }
      }
    }

    validateRequiredFieldsToStore.and(validateAtLeastOneIdentifierToStore)(inputFields)

  }

}

/**
  * Contains configuration for the DocumentRulesValidator
  * refData - Able to access reference data such as validation configuration
  */
object DocumentRulesValidator {
  case class CfgCtx(refData: RefDataAccessorsCore)
}
