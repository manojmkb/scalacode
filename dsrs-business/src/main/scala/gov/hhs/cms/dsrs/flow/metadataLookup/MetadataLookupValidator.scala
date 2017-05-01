package gov.hhs.cms.dsrs.flow.metadataLookup

import java.time.Instant

import gov.hhs.cms.arch.validation.ValidationResult
import gov.hhs.cms.dsrs.DSRSConstants._
import gov.hhs.cms.dsrs.flow.metadataLookup.MetadataLookupValidator.CfgCtx
import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator, ValidatorCore}
import org.json4s._

/**
  * Performs validation on a Json request for the MetadataLookup flow. Validates the dsrsId.
  */
class MetadataLookupValidator(cfgCtx : CfgCtx) extends ValidatorCore {

  private val refData = cfgCtx.refData.getRefData

  private def getLookupErrorPrefix = refData.getString(VALIDATION_ERROR_CONFIG_NAME, Instant.now(), "prefix", "lookup")

  def apply(json: JObject): ValidationResult = {
    // will validate the dsrsID (length, alphanumeric, string - and they year which is extracted from it)
    // should the year be extracted and added to the object?

    val result: ValidationResult = new ValidationResult()
    prefixValidationErrors(refData, result, getLookupErrorPrefix)
  }

}

/**
  * Contains configuration for the MetadataLookupValidator
  * refData - Able to access reference data such as validation configuration
  * documentRules - Performs validation for document-level rules
  * fieldRules - Performs validation for field-level rules
  */
object MetadataLookupValidator {
  case class CfgCtx(refData: RefDataAccessorsCore, documentRulesValidator: DocumentRulesValidator, fieldRulesValidator: FieldRulesValidator)
}
