package gov.hhs.cms.dsrs.refdata

import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}

import com.typesafe.config.Config

import java.time.Instant

import net.sf.ehcache.{Cache, CacheManager}

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by hbarot on 2/16/17.
  */

@RunWith(classOf[JUnitRunner])
class RefDataAccessorsSpec extends WordSpec with Matchers {

  val config: Config = ConfigUtil.loadConfig(
    ConfigUtil.getDefaultAppConfPath,
    ConfigUtil.getCurrEnvConfPath)

  val refDataCache: Cache = {
    val cfg = CacheUtil.createCacheMngrCfg(config, "refDataCache")
    val cacheManager = CacheManager.create(cfg)

    cacheManager.getCache("refDataCache")
  }

  "RefDataAccessors" should {
    val ehCacheRefConfigFacade: EhCacheRefConfigFacade =
      new EhCacheRefConfigFacade(ConfigUtil.getDefaultRefConfPath,
      refDataCache)

    def referenceData = new ReferenceDataImpl(ehCacheRefConfigFacade)
    val refDataAccessor = new RefDataAccessors(referenceData)

    "successfully grab a value from refdata cache" in {
      val result : Int = refDataAccessor.getRefData.getInt("testRefData", Instant.now(), "testValue")
      result shouldBe 1
    }
  }
}

