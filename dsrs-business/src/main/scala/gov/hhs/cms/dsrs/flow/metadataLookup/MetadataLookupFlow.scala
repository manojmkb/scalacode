package gov.hhs.cms.dsrs.flow.metadataLookup

import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import gov.hhs.cms.dsrs.flow.metadataLookup.MetadataLookupFlow.CfgCtx
import gov.hhs.cms.dsrs.YearExtractorUtil
import gov.hhs.cms.dsrs.DSRSConstants._
import gov.hhs.cms.arch.validation.ValidationResult
import gov.hhs.cms.arch.persistence.dynamodb.GetFromDynamoDbDaf

import com.typesafe.config.Config

import org.json4s._
import org.json4s.{DefaultFormats, JObject}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements the validate method which passes a Json request to the MetadataLookupValidator for validation
  */
class MetadataLookupFlow (cfgCtx : CfgCtx) extends MetadataLookupFlowCore {

  implicit val formats = DefaultFormats

  override def validate(json: JObject): ValidationResult = cfgCtx.validator(json)

  override def getFromDynamo(json: JObject): Future[JValue] = {
    val year = YearExtractorUtil.getYearFromDsrsId(json)

    val tableName = cfgCtx.config.getString("dynamodb.prefix") + METADATA_TABLE_NAME + year
    cfgCtx.getFromDynamoDbDaf(tableName, (json \ DSRS_ID_FIELD_NAME).extract[String])
  }

  override def removeAuditFields(dbGet: Future[JObject]): Future[JObject] = {
    val auditFields = List(LAST_MODIFIED_DATE_TIME_FIELD_NAME, LAST_MODIFIED_USER_FIELD_NAME,
      CREATED_USER_FIELD_NAME, CREATION_DATE_TIME_FIELD_NAME, DOCUMENT_URI_FIELD_NAME)
    dbGet.map(value => {
      value
        .removeField(item => auditFields.contains(item._1))
        .asInstanceOf[JObject]
    })
  }

  override def replaceIdWithIdentifier(result: Future[JObject]): Future[JObject] = {

    result.map(value => {
      value
        .merge(JObject(List((DSRS_ID_RESPONSE_FIELD_NAME, value \ DSRS_ID_FIELD_NAME))))
        .removeField(item => item._1 equals DSRS_ID_FIELD_NAME)
        .asInstanceOf[JObject]
    })
  }

}

/**
  * Contains configuration for the MetadataLookupFlow
  * refData - Able to access reference data such as validation configuration
  * validator - Performs all validation for the Document Store API
  */
object MetadataLookupFlow {
  case class CfgCtx(config: Config, refData: RefDataAccessorsCore, validator: MetadataLookupValidator, getFromDynamoDbDaf: GetFromDynamoDbDaf)
}