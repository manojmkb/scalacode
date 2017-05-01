package gov.hhs.cms.dsrs.validators

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.dsrs.validators.DocumentRulesValidator.{CfgCtx => DocCfgCtx}
import gov.hhs.cms.dsrs.validators.DocumentRulesValidatorSpec._
import gov.hhs.cms.dsrs.refdata.RefDataAccessors

import com.typesafe.config.Config

import net.sf.ehcache.{Cache, CacheManager}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by dev on 2/10/17.
  */
class DocumentRulesValidatorSpec extends WordSpec with Matchers {

  val docRules = documentRulesValidatorWithRefData()

  "The DocumentRulesValidator" should {

    "return an empty ValidationResult if all the validations are passed" in {
      // This input is valid
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = docRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 0
    }

    "return 1 ValidationError in the ValidationResult if a required common metadata field is missing" in {
      // This input is missing the required common metadata field sourceSystemCode
      val node = parse("""{"documentCategoryCode":"NOTICE", "fileSize":"4",
                         "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                         "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = docRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1
    }

    "return 1 ValidationError in the ValidationResult if a required extended metadata field is missing" in {
      // This input is missing the required extended metadata field documentSubcategoryCode
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                         "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                         "personTrackingNumber":"12345"}""")

      val result = docRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1
    }

    "return 1 ValidationError in the ValidationResult if there are no identifiers" in {
      // This input is missing identifiers
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "documentSubcategoryCode":"MOEN"}""")

      val result = docRules(node.asInstanceOf[JObject], "NOTICE")

      result.getErrors.size() shouldBe 1
    }
  }
}

object DocumentRulesValidatorSpec {
  def documentRulesValidatorWithRefData() = {

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

    val docCfgCtx = DocCfgCtx(refDataAccessor)

    new DocumentRulesValidator(docCfgCtx)
  }
}
