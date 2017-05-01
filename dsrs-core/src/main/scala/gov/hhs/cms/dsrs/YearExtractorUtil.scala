package gov.hhs.cms.dsrs

import org.json4s.{DefaultFormats, JValue}
import gov.hhs.cms.dsrs.DSRSConstants._

/**
  * Return the year from the appropriate field
  */
object YearExtractorUtil {

  implicit val formats = DefaultFormats // Required to use extract() on json4s objects

  /**
    * Return the year from the document creation date time
    * @param json the object from the the post request
    * @return
    */
  def getYearFromDocumentCreationDateTime(json: JValue): String = {
    val documentCreationDateTimeString = (json \ "documentCreationDateTime").extract[String]
    val yearRegex = """^\d{4}""".r
    val year: Option[String] = yearRegex.findFirstIn(documentCreationDateTimeString)
    require(year.isDefined) // Year should have been found otherwise the passed documentCreationDateTime was bad!
    year.get
  }

  /**
    * Return the year from the dsrs id
    * @param json the object from the get request
    * @return
    */
  def getYearFromDsrsId(json: JValue): String = {
    val id = (json \ "dsrsId").extract[String]
    id.takeRight(NUMBER_OF_DIGITS_IN_A_YEAR)
  }
}