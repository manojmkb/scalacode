package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.refdata.ReferenceData
import gov.hhs.cms.dsrs.YearExtractorUtil
import gov.hhs.cms.dsrs.DSRSConstants._

import scala.collection.JavaConverters._

import org.json4s.JsonDSL._
import org.json4s._
import java.time.Instant

/**
  * Extracts a list of valid identifiers provided in the input used for storing into the search table.
  * @author Charlie Davis, Taylor Mattison
  */
object ExtractSearchAttributesBF {
  implicit val formats = DefaultFormats

  def apply(json: JObject, refData: ReferenceData, dsrsId: String): List[JObject] = {
    val documentCategory: String = (json \ DOCUMENT_CATEGORY_CODE_FIELD_NAME).extract[String]
    val validIdentifiers: Set[String] = refData.getStringList(documentCategory, Instant.now(),
      DOCUMENT_RULES_CONFIG, SEARCH_ATTRIBUTES_CONFIG_RULE).asScala.toSet
    val providedIdentifiers: Map[String, String] = json.extract[Map[String, String]].filter(in => validIdentifiers.contains(in._1))

    providedIdentifiers.map(id => {
      (SEARCH_ATTRIBUTE_FIELD_NAME -> (id._1 + ":" + id._2)) ~
        (DSRS_ID_FIELD_NAME -> dsrsId) ~
        (YEAR_FIELD_NAME -> YearExtractorUtil.getYearFromDocumentCreationDateTime(json)) ~
        (SOURCE_SYSTEM_CODE_FIELD_NAME -> json \ SOURCE_SYSTEM_CODE_FIELD_NAME) ~
        (DOCUMENT_CATEGORY_CODE_FIELD_NAME -> documentCategory) ~
        (DOCUMENT_SUBCATEGORY_CODE_FIELD_NAME -> json \ DOCUMENT_SUBCATEGORY_CODE_FIELD_NAME)
    }).toList
  }
}

