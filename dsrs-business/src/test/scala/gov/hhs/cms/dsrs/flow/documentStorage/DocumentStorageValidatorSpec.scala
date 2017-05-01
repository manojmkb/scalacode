package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.dsrs.flow.documentStorage.DocumentStorageValidator.CfgCtx
import gov.hhs.cms.dsrs.validators.DocumentRulesValidator.{CfgCtx => DocCfgCtx}
import gov.hhs.cms.dsrs.validators.FieldRulesValidator.{CfgCtx => FieldCfgCtx}
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator}
import gov.hhs.cms.dsrs.refdata.RefDataAccessors
import com.typesafe.config.Config
import net.sf.ehcache.{Cache, CacheManager}
import org.scalatest.{Matchers, WordSpec}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import DocumentStorageValidatorSpec._


/**
  * Created by dev on 2/10/17.
  */
class DocumentStorageValidatorSpec extends WordSpec with Matchers {

 val docValidator = documentStorageValidatorWithRefData()

  "The DocumentStorageValidator when validating an input" should {

    "return an empty ValidationResult if all the validations (document and field) are passed" in {

      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = docValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 0
    }

    "return 2 ValidationErrors if both document validations are failed" in {

      //missingRequiredField documentSubcategoryCode and atLeastOneIdentifier with no ptn
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg"
                                                }""")

      val result = docValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 2
    }

    "return 3 ValidationErrors if all types of field validations fail" in {

      //fileSize wrong dataType, documentSubcategoryCode not allowedValue, documentFileName fails regex
      val node = parse("""{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":"4",
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"my~File.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"ABC"}""")

      val result = docValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 3
    }

    "return 1 ValidationErrors if there is no documentCategoryCode" in {

      val node = parse("""{"sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = docValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 1
    }

    "return 1 ValidationErrors if documentCategoryCode is COMMON" in {

      val node = parse("""{"documentCategoryCode":"COMMON", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345", "documentSubcategoryCode":"MOEN"}""")

      val result = docValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 1
    }
  }
}

object DocumentStorageValidatorSpec {
  def documentStorageValidatorWithRefData() = {

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
    val fieldCfgCtx = FieldCfgCtx(refDataAccessor)
    val docRules = new DocumentRulesValidator(docCfgCtx)
    val fieldRules = new FieldRulesValidator(fieldCfgCtx)
    val cfgCtx = CfgCtx(refDataAccessor, docRules, fieldRules)

    new DocumentStorageValidator(cfgCtx)
  }
}
