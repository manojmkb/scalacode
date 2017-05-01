package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.persistence.dynamodb.SaveToDynamoDbDaf
import gov.hhs.cms.arch.validation.ValidationResult
import gov.hhs.cms.dsrs.flow.documentStorage.DocumentStorageFlow.CfgCtx
import gov.hhs.cms.dsrs.refdata.RefDataAccessorsCore
import gov.hhs.cms.dsrs.YearExtractorUtil
import gov.hhs.cms.dsrs.DSRSConstants._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

import com.typesafe.config.Config
import org.json4s.JsonDSL._
import org.json4s._

/**
  * Implements the validate method which passes a Json request to the DocumentStorageValidator
  * for validation
  */
class DocumentStorageFlow(cfgCtx: CfgCtx) extends DocumentStorageFlowCore {
  implicit val formats = DefaultFormats

  def tableNamePrefix: String = cfgCtx.config.getString("dynamodb.prefix")

  override def validate(json: JObject): ValidationResult = cfgCtx.validator(json)

  override def storeToDynamo(json: JObject): Future[JObject] =
    for {
      dsrsId <- storeMetadataToDynamo(json)
      json <- storeSearchAttributesToDynamo(json, dsrsId)
    } yield {
      JObject(JField(DSRS_ID_RESPONSE_FIELD_NAME, JString(dsrsId)))
    }

  private def storeMetadataToDynamo(json: JObject): Future[String] = {
    val tableName: String = tableNamePrefix + METADATA_TABLE_NAME +
      YearExtractorUtil.getYearFromDocumentCreationDateTime(json)

    cfgCtx.saveToDynamoDB(tableName, json)
      .map(result => (result \ DSRS_ID_FIELD_NAME).extract[String])
  }

  private def storeSearchAttributesToDynamo(json: JObject, dsrsId: String): Future[JValue] = {
    val tableName: String = tableNamePrefix + SEARCH_TABLE_NAME
    val searchAttributesList: List[JObject] = ExtractSearchAttributesBF(
      json,
      cfgCtx.refData.getRefData,
      dsrsId)
    cfgCtx.saveToDynamoDB(tableName, searchAttributesList)
  }

  override def addFileInformationToJson(json: JObject, s3FilePath: String): JObject = {
    json ~
      (FILE_FORMAT_FIELD_NAME -> getExtensionFromFilePath(s3FilePath)) ~
      (DOCUMENT_URI_FIELD_NAME -> s3FilePath)
  }

  private def getExtensionFromFilePath(filePath: String): String = {
    val extensionRegex = """(?<=\.)[^.\/]+$""".r
    val extension = extensionRegex.findFirstIn(filePath)
    assert(
      extension
        .isDefined) // Extension must be valid or the filename passed in was not validated properly!
    extension.get.toUpperCase // Map toUpperCase so that file.jpg is the same as file.JPG
  }

}

/**
  * Contains configuration for the DocumentStorageFlow
  * refData - Able to access reference data such as validation configuration
  * validator - Performs all validation for the Document Store API
  */
object DocumentStorageFlow {

  case class CfgCtx(
      config: Config,
      refData: RefDataAccessorsCore,
      validator: DocumentStorageValidator,
      saveToDynamoDB: SaveToDynamoDbDaf)

}