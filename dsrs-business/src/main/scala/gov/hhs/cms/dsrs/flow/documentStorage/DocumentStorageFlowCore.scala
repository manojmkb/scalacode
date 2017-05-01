package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.validation.ValidationResult
import org.json4s.JObject
import org.json4s.JsonAST.JString

import scala.concurrent.Future

/**
  * Contains the implementation of the apply method for the DocumentStorageFlow which accepts input, validates it, and
  * echos the result if successful.
  */
trait DocumentStorageFlowCore extends (((String, JObject)) => Future[JObject]) {

  /**
    * Performs validation on the input by applying API, Document, and Field level validation rules.
    * @param json The JSON request to validate
    * @return A ValidationResult containing a ValidationError for each validation rule that is violated
    */
  def validate(json: JObject): ValidationResult

  /**
    * Uses the Arch-NG persistence framework to store the metadata in DynamoDB
    * @param json The JSON request to insert
    * @return The DSRS ID of the stored record
    */
  def storeToDynamo(json: JObject): Future[JObject]

  /**
    * Adds fields to the json to store into Dynamo which were not included in the request from the user
    * @param json The starting point of the json
    * @param s3Filename The s3 filename to add
    * @return A JObject containing json + any generated fields
    */
  def addFileInformationToJson(json: JObject, s3Filename: String): JObject

  override def apply(input: (String, JObject)): Future[JObject] = {
      val (s3Filename, jsonRequest) = input
      validate(jsonRequest).throwIfFailed("The request failed one or more validation rules.")
      val updatedJson = addFileInformationToJson(jsonRequest, s3Filename)
      storeToDynamo(updatedJson)

      val hashed_pi = sha1(Array(3,1,4,1,5,9,2,6,5,3,5).map(_.toByte))
      Future.successful(JObject(List("InsecureHashedPI" -> JString(hashed_pi))))
  }

  def sha1(bytes: Array[Byte]): String = {
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-1")
    digest.reset()
    digest.update(bytes)
    digest.digest().map(0xFF & _).map { "%02x".format(_) }.foldLeft("") { _ + _ }
  }

}
