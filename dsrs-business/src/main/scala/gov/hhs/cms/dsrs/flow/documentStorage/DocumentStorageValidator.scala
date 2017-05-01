package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.validation._
import gov.hhs.cms.dsrs.flow.documentStorage.DocumentStorageValidator.CfgCtx
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator, ValidatorCore}
import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import java.time.Instant

import gov.hhs.cms.dsrs.DSRSConstants._

import scala.util.Try
import org.json4s.JsonAST.JNothing
import org.json4s.JObject

/**
  * Performs validation on a Json request for the DocumentStorage flow. First pre-validates the documentCategoryCode,
  * then applies document and field level validation rules before returning the ValidationResult
  */
class DocumentStorageValidator(cfgCtx : CfgCtx) extends ValidatorCore {

  private val refData = cfgCtx.refData.getRefData

  private def getStoreErrorPrefix = refData.getString(VALIDATION_ERROR_CONFIG_NAME, Instant.now(), "prefix", "store")

  def apply(json: JObject): ValidationResult = {

    val result = {
      if (validateDocumentCategoryProvided(json).failed()) {
        validateDocumentCategoryProvided(json)
      }
      else if (validateDocumentCategory((json \ DOCUMENT_CATEGORY_CODE_FIELD_NAME).extract[String]).failed()) {
        validateDocumentCategory((json \ DOCUMENT_CATEGORY_CODE_FIELD_NAME).extract[String])
      }
      else {
        val documentCategory: String = (json \ DOCUMENT_CATEGORY_CODE_FIELD_NAME).extract[String]
        cfgCtx.documentRulesValidator(json, documentCategory).merge(cfgCtx.fieldRulesValidator(json, documentCategory))
      }
    }

    prefixValidationErrors(refData, result, getStoreErrorPrefix)
  }

  /**
    * Validates that the documentCateogry has been provided
    * @return validator
    */
  def validateDocumentCategoryProvided: Validator[JObject] = new Validator[JObject] {
    override def apply(json: JObject): ValidationResult = {
      val result = new ValidationResult()
      if ((json \ DOCUMENT_CATEGORY_CODE_FIELD_NAME) == JNothing) {
        result.add(new ValidationError(DOCUMENT_CATEGORY_CODE_FIELD_NAME,
          getErrorCodeFromRefData(refData, REQUIRED_TO_STORE_CONFIG_RULE)))
      }
      result
    }
  }

  /**
    * Validates that the documentCategory is valid
    * @return validator
    */
  def validateDocumentCategory: Validator[String] = new Validator[String] {
    override def apply(documentCategory: String): ValidationResult = {
      val result = new ValidationResult()
      if (Try(refData.isValidPath(documentCategory, Instant.now(), FIELD_RULES_CONFIG)).isFailure ||
        documentCategory == COMMON_CONFIG_NAME) {
        result.add(new ValidationError(DOCUMENT_CATEGORY_CODE_FIELD_NAME,
          getErrorCodeFromRefData(refData, "unrecognizedReferenceType")))
      } // improve arch-ref-data to have an isValidContext method, and use it here to check input.
      result
    }
  }

}

/**
  * Contains configuration for the DocumentStorageValidator
  * refData - Able to access reference data such as validation configuration
  * documentRules - Performs validation for document-level rules
  * fieldRules - Performs validation for field-level rules
  */
object DocumentStorageValidator {
  case class CfgCtx(refData: RefDataAccessorsCore, documentRulesValidator: DocumentRulesValidator, fieldRulesValidator: FieldRulesValidator)
}