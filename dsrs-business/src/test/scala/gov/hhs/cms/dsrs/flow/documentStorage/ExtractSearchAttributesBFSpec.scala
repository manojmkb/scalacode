package gov.hhs.cms.dsrs.flow.documentStorage

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.refdata.ReferenceData
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.dsrs.refdata.RefDataAccessors

import com.typesafe.config.Config
import net.sf.ehcache.{Cache, CacheManager}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by dev on 3/8/17.
  */
class ExtractSearchAttributesBFSpec extends WordSpec with Matchers {

  "The ExtractSearchAttributesBF" should {

    val nodeWithOneId = parse(
      """{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":"4",
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00",
                                                "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345",
                                                "documentSubcategoryCode":"MOEN"}""")

    val nodeWithTwoIds = parse(
      """{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":"4",
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00",
                                                "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345",
                                                "insuranceApplicationIdentifier":"123",
                                                "documentSubcategoryCode":"MOEN"}""")

    "successfully return a list with one identifier" in {
      ExtractSearchAttributesBF(
        nodeWithOneId.asInstanceOf[JObject],
        ExtractSearchAttributesBFSpec.refData,
        "123").size shouldBe 1
    }

    "successfully return a list with two identifier" in {
      ExtractSearchAttributesBF(
        nodeWithTwoIds.asInstanceOf[JObject],
        ExtractSearchAttributesBFSpec.refData,
        "123").size shouldBe 2
    }
  }
}

object ExtractSearchAttributesBFSpec {
  def refData(): ReferenceData = {
    System.setProperty("properties.home", "./src/test/resources/config")
    System.setProperty("environment.properties", "./src/test/resources/config/env/env.properties")

    val config: Config = ConfigUtil.loadConfig(
      ConfigUtil.getCurrEnvConfPath, ConfigUtil.getDefaultRefConfPath)

    val REF_DATA_CACHE_NAME = "refDataCache"
    val cfg = CacheUtil.createCacheMngrCfg(config, REF_DATA_CACHE_NAME)
    val cacheManager = CacheManager.create(cfg)

    val refDataCache: Cache = cacheManager.getCache(REF_DATA_CACHE_NAME)

    val ehCacheRefConfigFacade: EhCacheRefConfigFacade = new EhCacheRefConfigFacade(
      ConfigUtil.getDefaultRefConfPath,
      refDataCache)

    val refData = new ReferenceDataImpl(ehCacheRefConfigFacade)
    new RefDataAccessors(refData).refData
  }
}