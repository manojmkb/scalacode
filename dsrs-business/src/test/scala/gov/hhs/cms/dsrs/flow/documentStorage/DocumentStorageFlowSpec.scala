package gov.hhs.cms.dsrs.flow.documentStorage

import java.time.Instant

import com.amazonaws.auth.BasicAWSCredentials
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.typesafe.config.Config
import gov.hhs.cms.arch.cache.CacheUtil
import gov.hhs.cms.arch.config.ConfigUtil
import gov.hhs.cms.arch.persistence.dynamodb.{DynamoDbDaf, DynamoDbRestDataSource, DynamoDbRestRequest, SaveToDynamoDbDaf}
import gov.hhs.cms.arch.refdata.impl.{EhCacheRefConfigFacade, ReferenceDataImpl}
import gov.hhs.cms.arch.security.{SecurityContext, SecurityContextHolder}
import gov.hhs.cms.dsrs.DSRSConstants
import gov.hhs.cms.dsrs.flow.documentStorage.DocumentStorageFlowSpec._
import gov.hhs.cms.dsrs.flow.documentStorage.DocumentStorageValidator.{CfgCtx => ValCfgCtx}
import gov.hhs.cms.dsrs.refdata.RefDataAccessors
import gov.hhs.cms.dsrs.validators.DocumentRulesValidator.{CfgCtx => DocCfgCtx}
import gov.hhs.cms.dsrs.validators.FieldRulesValidator.{CfgCtx => FieldCfgCtx}
import gov.hhs.cms.dsrs.validators.{DocumentRulesValidator, FieldRulesValidator}

import scala.concurrent.Future
import java.time.Instant

import com.amazonaws.auth.BasicAWSCredentials
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.typesafe.config.Config
import net.sf.ehcache.{Cache, CacheManager}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
  * Created by dev on 3/1/17.
  */
class DocumentStorageFlowSpec extends WordSpec with Matchers with BeforeAndAfter {

  before {
    SecurityContextHolder.setSecurityContext(
      new SecurityContext(
        "testUser",
        "testRole",
        "testClient",
        "1",
        Instant.now()))
  }

  val docFlow: DocumentStorageFlow = documentStorageFlowWithRefData()

  "The DocumentStorageFlow " should {

    "take in a filename and metadata object and eventually return a completed future" in {

      val fileName = "fileName.txt"
      val node = parse(
        """{"documentCategoryCode":"NOTICE", "sourceSystemCode":"FFM_EE", "fileSize":4,
                                                "documentCreationDateTime":"2016-02-18T12:00:00+00:00", "documentFileName":"myFile.jpg",
                                                "personTrackingNumber":"12345",
                                                "documentSubcategoryCode":"MOEN"}""")

      val docFlowResult = docFlow(fileName, node.asInstanceOf[JObject])

      Await.result(docFlowResult, 1000 millis)

      docFlowResult should not be null

    }
  }

}

object DocumentStorageFlowSpec {
  def documentStorageFlowWithRefData(): DocumentStorageFlow = {
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
    val refDataAccessor = new RefDataAccessors(refData)

    val docCfgCtx = DocCfgCtx(refDataAccessor)
    val fieldCfgCtx = FieldCfgCtx(refDataAccessor)
    val docRules = new DocumentRulesValidator(docCfgCtx)
    val fieldRules = new FieldRulesValidator(fieldCfgCtx)
    val valCfgCtx = ValCfgCtx(refDataAccessor, docRules, fieldRules)
    val valRules = new DocumentStorageValidator(valCfgCtx)

    class SomeMockDataSource extends MockDataSource {
      override def post(request: DynamoDbRestRequest): Future[JValue] = {
        Future.successful(parse(s"""{"dsrsId": "123"}""".stripMargin))
      }
    }

    val saveToDynamoDbDaf = SaveToDynamoDbDaf(
      DynamoDbDaf.CfgCtx(
        new SomeMockDataSource,
        "pass", "partitionKey", encryptionAttributes = () => Set(),
        idPrefixGenerator = (_) => "", idSuffixGenerator = (_) => ""))

    val cfgCtx = DocumentStorageFlow.CfgCtx(config, refDataAccessor, valRules, saveToDynamoDbDaf)
    new DocumentStorageFlow(cfgCtx)
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