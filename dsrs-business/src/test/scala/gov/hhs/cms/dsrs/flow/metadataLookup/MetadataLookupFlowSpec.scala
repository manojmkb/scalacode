package gov.hhs.cms.dsrs.flow.metadataLookup

import java.time.Instant

import com.amazonaws.auth.BasicAWSCredentials
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.typesafe.config.Config
import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.persistence.core.exception.{NoObjectFoundException, PersistenceInternalErrorException}
import gov.hhs.cms.arch.persistence.dynamodb._
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.arch.security.{SecurityContext, SecurityContextHolder}
import gov.hhs.cms.dsrs.flow.metadataLookup.MetadataLookupFlowSpec._
import gov.hhs.cms.dsrs.refdata.RefDataAccessors
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator}
import net.sf.ehcache.{Cache, CacheManager}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by dev on 3/3/17.
  */
class MetadataLookupFlowSpec extends WordSpec with Matchers with BeforeAndAfter {

  before {
    SecurityContextHolder.setSecurityContext(
      new SecurityContext(
        "testUser",
        "testRole",
        "testClient",
        "1",
        Instant.now()))
  }
  val lookFlow = metadataLookupFlowWithRefData()
  val lookFlowFailed = metadataLookupFlowWithRefDataFailed()

  "The MetadataLookupFlow " should {

    "take in a dsrsId and return a successful future" in {

      val node = parse("""{"dsrsId":"123abcd2017"}""")

      val result: Future[JObject] = lookFlow(node.asInstanceOf[JObject])

      Await.result(result, 1000 millis)

      result should not be null
    }

    "take in a dsrsId and return a failed future" in {

      val node = parse("""{"dsrsId":"123abcd2017"}""")

      val result: Future[JObject] = lookFlowFailed(node.asInstanceOf[JObject])

      a [NoObjectFoundException] must be thrownBy {
        Await.result(result, 1000 millis)
      }
    }
  }

}


object MetadataLookupFlowSpec {

  def setUpConfig(): Config = {
    System.setProperty("properties.home", "./src/test/resources/config")
    System.setProperty("environment.properties", "./src/test/resources/config/env/env.properties")

    ConfigUtil.loadConfig(
    ConfigUtil.getCurrEnvConfPath, ConfigUtil.getDefaultRefConfPath)
  }

  def setUpRefData(config: Config): RefDataAccessors = {
    val REF_DATA_CACHE_NAME = "refDataCache"
    val cfg = CacheUtil.createCacheMngrCfg(config, REF_DATA_CACHE_NAME)
    val cacheManager = CacheManager.create(cfg)

    val refDataCache: Cache = cacheManager.getCache(REF_DATA_CACHE_NAME)

    val ehCacheRefConfigFacade: EhCacheRefConfigFacade = new EhCacheRefConfigFacade(
      ConfigUtil.getDefaultRefConfPath,
      refDataCache)

    val refData = new ReferenceDataImpl(ehCacheRefConfigFacade)
    new RefDataAccessors(refData)
  }

  def setUpValidator(refDataAccessor: RefDataAccessors): MetadataLookupValidator = {
    val docCfgCtx = DocumentRulesValidator.CfgCtx(refDataAccessor)
    val fieldCfgCtx = FieldRulesValidator.CfgCtx(refDataAccessor)
    val docRules = new DocumentRulesValidator(docCfgCtx)
    val fieldRules = new FieldRulesValidator(fieldCfgCtx)
    val valCfgCtx = MetadataLookupValidator.CfgCtx(refDataAccessor, docRules, fieldRules)
    new MetadataLookupValidator(valCfgCtx)
  }

  def metadataLookupFlowWithRefData(): MetadataLookupFlow = {

    val config = setUpConfig()
    val refDataAccessors = setUpRefData(config)
    val valRules = setUpValidator(refDataAccessors)

    class SomeMockDataSource extends MockDataSource {
      override def post(request: DynamoDbRestRequest): Future[JValue] = {
        Future.successful(parse(s"""[{"result": "Some result"}]""".stripMargin))
      }
    }

    val getFromDynamoDbDaf = GetFromDynamoDbDaf(
      DynamoDbDaf.CfgCtx(
        new SomeMockDataSource,
        "pass", "partitionKey", encryptionAttributes = () => Set(),
        idPrefixGenerator = (_) => "", idSuffixGenerator = (_) => ""))

    val cfgCtx = MetadataLookupFlow.CfgCtx(config, refDataAccessors, valRules, getFromDynamoDbDaf)
    new MetadataLookupFlow(cfgCtx)
  }

    def metadataLookupFlowWithRefDataFailed(): MetadataLookupFlow = {

      val config = setUpConfig()
      val refDataAccessors = setUpRefData(config)
      val valRules = setUpValidator(refDataAccessors)

      class SomeMockDataSource extends MockDataSource {
        override def post(request: DynamoDbRestRequest): Future[JValue] = {
          Future.failed(new PersistenceInternalErrorException("Failed DynamoDB Call"))
        }
      }

    val getFromDynamoDbDaf = GetFromDynamoDbDaf(
      DynamoDbDaf.CfgCtx(
        new SomeMockDataSource,
        "pass", "partitionKey", encryptionAttributes = () => Set(),
        idPrefixGenerator = (_) => "", idSuffixGenerator = (_) => ""))

    val cfgCtx = MetadataLookupFlow.CfgCtx(config, refDataAccessors, valRules, getFromDynamoDbDaf)
    new MetadataLookupFlow(cfgCtx)
  }
}

abstract class MockDataSource extends DynamoDbRestDataSource(
  DynamoDbRestDataSource.CfgCtx(
    "http://127.0.0.1:50000/mockDynamodb",
    "DSRSIdentifier",
    () => new BasicAWSCredentials("AccessKey", "SecretKey"),
    null)) {
  override def createWriteRequest(tableName: String)
                                 (body: JValue): DynamoDbRestRequest = new DynamoDbRestRequest(
    null, body, null, null, null, null, null)

  override def createSearchRequest(tableName: String)
                                  (criteria: Map[String, Any]): DynamoDbRestRequest = new DynamoDbRestRequest(
    null, parse(createJsonFromMap(criteria)), null, null, null, null, null)

  private def createJsonFromMap(m: Map[String, Any]): String = {
    val json = JsonNodeFactory.instance.objectNode()
    m.map(
      kv => {
        if (kv._2.isInstanceOf[String]) json.putPOJO(kv._1, "\"" + kv._2 + "\"")
        else json.putPOJO(kv._1, kv._2)
      })
    json.toString
  }
}