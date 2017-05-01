package gov.hhs.cms.dsrs

import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by dev on 3/6/17.
  */
class YearExtractorUtilSpec extends WordSpec with Matchers{

  "The YearExtractorUtil" should {

    "successfully extract a year from a Post" in {
      val year2016Json = parse("""{"documentCreationDateTime":"2016-02-18T12:00:00+00:00"}""")
      val year2015Json = parse("""{"documentCreationDateTime":"2015-02-18T12:00:00+00:00"}""")
      YearExtractorUtil.getYearFromDocumentCreationDateTime(year2016Json) shouldBe "2016"
      YearExtractorUtil.getYearFromDocumentCreationDateTime(year2015Json) shouldBe "2015"
    }

    "successfully extract a year from a Get" in {
      val year2016Json = parse("""{"dsrsId":"abc123fgh456qwe2016"}""")
      val year2015Json = parse("""{"dsrsId":"abc123fgh456qwe2015"}""")
      YearExtractorUtil.getYearFromDsrsId(year2016Json) shouldBe "2016"
      YearExtractorUtil.getYearFromDsrsId(year2015Json) shouldBe "2015"
    }
  }
}
