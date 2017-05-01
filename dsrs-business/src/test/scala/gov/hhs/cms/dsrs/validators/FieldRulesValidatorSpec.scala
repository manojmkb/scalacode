package gov.hhs.cms.dsrs.validators

import com.typesafe.config.Config

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.exception.ConfigurationException
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.dsrs.refdata.RefDataAccessors
import gov.hhs.cms.dsrs.validators.FieldRulesValidator.{CfgCtx => FieldCfgCtx}
import gov.hhs.cms.dsrs.validators.FieldRulesValidatorSpec._

import net.sf.ehcache.{Cache, CacheManager}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by dev on 2/10/17.
  */
class FieldRulesValidatorSpec extends WordSpec with Matchers {

  val fieldRules = fieldRulesValidatorWithRefData()

  "The FieldRulesValidator" should {

    "return an empty ValidationResult if all the validations are passed" in {

      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 0
    }

    "return 1 ValidationErrors if there is an unrecognized field" in {

      //has extra field iceCream
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN", "iceCream":"chocolate"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1
    }

    "return an empty ValidationResult for all data type checks" in {

      val node = parse("""{"stringItem":"string string", "numberItem":4, "numberItem2":1.2,
                                                "booleanItem":true, "arrayItem":["item1", "item2", "item3"],
                                                "objectItem":{"key":"value"}}""")

      val result = fieldRules(node.asInstanceOf[JObject], "TYPE_TEST")

      result.getErrors.size() shouldBe 0
    }


    "return 1 ValidationError if the data type does not match" in {

      val node = parse("""{"stringItem":6, "numberItem":4, "numberItem2":1.2,
                                                "booleanItem":true, "arrayItem":["item1", "item2", "item3"],
                                                "objectItem":{"key":"value"}}""")

      val result = fieldRules(node.asInstanceOf[JObject], "TYPE_TEST")

      result.getErrors.size() shouldBe 1
    }

    "return 1 ValidationError if the data type is unrecognized" in {

      val node = parse("""{"stringItem":null, "numberItem":4, "numberItem2":1.2,
                                                "booleanItem":true, "arrayItem":["item1", "item2", "item3"],
                                                "objectItem":{"key":"value"}}""")

      val result = fieldRules(node.asInstanceOf[JObject], "TYPE_TEST")

      result.getErrors.size() shouldBe 1
    }

    "return 1 ValidationError if the input is not allowed" in {

      //subcategory code is ABC
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"ABC"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1
    }

    "throw a configuration error if the error is not found" in {

      //subcategory code is ABC
      val node = parse("""{"badError":6}""")

      a [ConfigurationException] must be thrownBy {
        fieldRules(node.asInstanceOf[JObject], "BAD_CONF")
      }

    }

    "throw a configuration error if the regex can not be found" in {

      //subcategory code is ABC
      val node = parse("""{"badRule":"stuff"}""")

      a [ConfigurationException] must be thrownBy {
        fieldRules(node.asInstanceOf[JObject], "BAD_CONF")
      }

    }

    "get 1 validation error when the fileSize is too big" in {

      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":11000000,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"abc123.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1

    }

    "get 1 validation error when the fileSize is too small" in {

      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":0,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"abc123.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1

      }

    "return 3 ValidationError if all tests fail" in {

      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":"4",
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"abc123~abc123abc123.pixar",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = fieldRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 3
    }

  }
}

object FieldRulesValidatorSpec {
  def fieldRulesValidatorWithRefData() = {

    System.setProperty("properties.home", "./src/test/resources/config")
    System.setProperty("environment.properties", "./src/test/resources/config/env/env.properties")

    val config: Config = ConfigUtil.loadConfig(
      ConfigUtil.getCurrEnvConfPath, ConfigUtil.getDefaultRefConfPath)

    val REF_DATA_CACHE_NAME = "refDataCache"
    val cfg = CacheUtil.createCacheMngrCfg(config, REF_DATA_CACHE_NAME)
    val cacheManager = CacheManager.create(cfg)

    val refDataCache: Cache = cacheManager.getCache(REF_DATA_CACHE_NAME)

    val ehCacheRefConfigFacade: EhCacheRefConfigFacade = new EhCacheRefConfigFacade(ConfigUtil.getDefaultRefConfPath,
      refDataCache)

    val refData = new ReferenceDataImpl(ehCacheRefConfigFacade)

    val refDataAccessor = new RefDataAccessors(refData)
    val fieldCfgCtx = FieldCfgCtx(refDataAccessor)

    new FieldRulesValidator(fieldCfgCtx)
  }
}
