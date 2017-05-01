package gov.hhs.cms.dsrs.flow.metadataLookup

import gov.hhs.cms.arch.persistence.core.exception.{NoObjectFoundException, PersistenceInternalErrorException}
import gov.hhs.cms.arch.validation.ValidationResult
import org.json4s.JObject
import org.json4s._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Contains the implementation of the apply method for the MetadataLookupFlow which accepts input, validates it, and
  * echos the result if successful.
  */
trait MetadataLookupFlowCore extends (((JObject)) => Future[JObject]) {

  /**
    * Performs validation on the input by applying API, Document, and Field level validation rules.
    * @param json The JSON request to validate
    * @return A ValidationResult containing a ValidationError for each validation rule that is violated
    */
  def validate(json: JObject): ValidationResult

  /**
    * Uses Arch-NG persistence framework to get metadata from DynamoDB
    * @param json The Json request
    * @return
    */
  def getFromDynamo(json: JObject): Future[JValue]

  /**
    * Removes audit fields from the result
    * @param dbGet the result from dynamo
    * @return
    */
  def removeAuditFields(dbGet: Future[JObject]): Future[JObject]

  /**
    * Replaces Id with Identifier
    * @param result the result from dynamo
    * @return
    */
  def replaceIdWithIdentifier(result: Future[JObject]): Future[JObject]

  override def apply(input: JObject): Future[JObject] = {

    validate(input).throwIfFailed("The request failed one or more validation rules.")
    val result = getFromDynamo(input)
      .map(_.asInstanceOf[JObject])
      // TODO: Remove when framework updated
      .recoverWith{
        case e:PersistenceInternalErrorException =>
          Future.failed(new NoObjectFoundException("DsrsIdentifier was not found in DB"))
      }
    replaceIdWithIdentifier(removeAuditFields(result))

  }

}
