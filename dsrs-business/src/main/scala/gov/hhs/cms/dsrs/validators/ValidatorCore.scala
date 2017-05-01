package gov.hhs.cms.dsrs.validators

import gov.hhs.cms.arch.refdata.ReferenceData
import gov.hhs.cms.arch.validation.{ValidationError, ValidationResult}
import org.json4s._
import java.time.Instant

import gov.hhs.cms.dsrs.DSRSConstants._

import scala.collection.JavaConverters._

/**
  * Handles common validation functions such as accessing refData and prefixing ValidationErrors
  */
trait ValidatorCore {

  implicit val formats = DefaultFormats

  /**
    * Adds prefixes to validation error list and throws validation errors if any exist
    *
    * @param configFile the document category code (the name of the file to access)
    * @param documentRule the name of the rule to look for
    * @return the set of validation rules found (or empty if none exist)
    */
  protected def getStringListIfExists(refData: ReferenceData, configFile: String, documentRule: String): Set[String] = {
    if (refData.isValidPath(configFile, Instant.now(), DOCUMENT_RULES_CONFIG, documentRule)) {
      refData.getStringList(configFile, Instant.now(), DOCUMENT_RULES_CONFIG, documentRule).asScala.toSet
    } else {
      Set.empty
    }
  }

  /**
    * Adds prefixes to validation error list and throws validation errors if any exist
    *
    * @param validator the validationResults
    * @param prefix    the prefix of the flow use in
    * @return ValidationResult with prefixes added
    */
  protected def prefixValidationErrors(refData: ReferenceData, validator: ValidationResult, prefix: String): ValidationResult = {
    val list: List[ValidationError] = validator.getErrors.asScala
      .map(s => new ValidationError(s.getPath, prefix + s.getErrorCode)).toList
    val javaList: java.util.List[ValidationError] = list.asJava
    val result = new ValidationResult() // ValidationResult(List<>) throws null pointer for converted scala lists
    result.add(javaList)
    result
  }

  /**
    * Retrieves the 3-digit validation error suffix for the provided error name
    * @param refData a refDataAccessor that can be used to read the validation configuration
    * @param errorName the name of the error to look up
    * @return The 3-digit numeric error code that is unique to the error
    */
  protected def getErrorCodeFromRefData(refData: ReferenceData, errorName: String): String = {
    refData.getString(VALIDATION_ERROR_CONFIG_NAME, Instant.now(), "errors", errorName, "code")
  }

}

