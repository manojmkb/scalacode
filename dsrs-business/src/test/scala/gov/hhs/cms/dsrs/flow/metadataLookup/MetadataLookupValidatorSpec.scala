package gov.hhs.cms.dsrs.flow.metadataLookup

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.dsrs.flow.metadataLookup.MetadataLookupValidator.CfgCtx
import gov.hhs.cms.dsrs.validators.DocumentRulesValidator.{CfgCtx => DocCfgCtx}
import gov.hhs.cms.dsrs.validators.FieldRulesValidator.{CfgCtx => FieldCfgCtx}
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator}
import gov.hhs.cms.dsrs.refdata.RefDataAccessors

import com.typesafe.config.Config

import net.sf.ehcache.{Cache, CacheManager}

import org.scalatest.{Matchers, WordSpec}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import MetadataLookupValidatorSpec._

/**
  * Created by dev on 3/3/17.
  */
class MetadataLookupValidatorSpec extends WordSpec with Matchers {

  val metaValidator = metadataLookupValidatorWithRefData()

  "The MetadataLookupValidator when validating an input" should {

    "return an empty ValidationResult if all the validations (dsrsId) are passed" in {

      val node = parse("""{"dsrsId":"123abc2017"}""")

      val result = metaValidator(node.asInstanceOf[JObject])

      result.getErrors.size() shouldBe 0
    }
  }
}

object MetadataLookupValidatorSpec {
  def metadataLookupValidatorWithRefData() = {

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

    new MetadataLookupValidator(cfgCtx)
  }
}

