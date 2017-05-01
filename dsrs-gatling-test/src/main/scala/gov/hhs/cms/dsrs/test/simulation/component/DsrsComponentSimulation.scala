package gov.hhs.cms.dsrs.test.simulation.component

import gov.hhs.cms.arch.test.gatling.HttpJsonNodeCheckBuilder._
import gov.hhs.cms.dsrs.test.simulation.common.DSRSBaseSimulation
import io.gatling.core.Predef._
import io.gatling.core.feeder.RecordSeqFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._

/**
  * Created by armaun.mirshamsi on 1/17/2017.
  */
class DsrsComponentSimulation extends
  DSRSBaseSimulation("ffm-gatling-env.dsrs.test.simulation.component.DsrsComponentSimulation"){

  val feeder6957 = jsonFile(feederFolder.concat("/R1.0/S2/US6957-feeder.json")).queue
  val feeder7047 = jsonFile(feederFolder.concat("/R1.0/S2/US7047-feeder.json")).queue

  val sprint2List = List(
    DsrsComponentSimulation
      .postRequestJsonNodeDsrsScenario("Sprint 2 - US6957",DsrsComponentSimulation.dsrsStoreHeaders,feeder6957)
      .inject(atOnceUsers(simulationAtOnceUsers)).protocols(httpConf),
    DsrsComponentSimulation
      .postRequestJsonNodeDsrsScenario("Sprint 2 - US7047",DsrsComponentSimulation.dsrsStoreHeaders,feeder7047)
      .inject(atOnceUsers(simulationAtOnceUsers)).protocols(httpConf)
  )

  val simulationScenariosList = List.concat(
    sprint2List
  )

  setUp(simulationScenariosList).assertions(global.failedRequests.count.is(0))
}

object DsrsComponentSimulation extends
  DSRSBaseSimulation("ffm-gatling-env.dsrs.test.simulation.component.DsrsComponentSimulation"){

  val sourceSystem = gatlingSimulationConfiguration.getString(simulationPropertyKey("Source-System-Name"))
  val documentCategory = gatlingSimulationConfiguration.getString(simulationPropertyKey("Document-Category"))
  val documentSubcategory = gatlingSimulationConfiguration.getString(simulationPropertyKey("Document-Subcategory"))
  val fileSize = gatlingSimulationConfiguration.getString(simulationPropertyKey("File-Size"))

  val dsrsStoreHeaders = defaultHeaders + (
    "Source-System-Name" -> sourceSystem,
    "Document-Category" -> documentCategory,
    "Document-Subcategory" -> documentSubcategory,
    "File-Size" -> fileSize
    )

  def getRepeatTimes(feeder:RecordSeqFeederBuilder[Any], repeatTimes: Option[Int]): Int = {
    (feeder, repeatTimes) match {
      case (feeder, Some(repeat)) => repeat
      case _ => feeder.build.length
    }
  }

  def postRequestJsonNodeDsrsScenario(userStory: String, defaultHeaders: Map[String, String],
                                      feeder: RecordSeqFeederBuilder[Any], repeatTimes: Option[Int] = None): ScenarioBuilder = {

    val storeRequestError = exec(http("${TestCase} - Document Store")
                                 .post("/document")
                                 .formParam("requestBody", ELFileBody("${JsonRequestBodyStore}"))
                                 .formUpload("file", "${JsonRequestDocumentStore}")
                                 .headers(dsrsStoreHeaders)
                                 .signatureCalculator((session) => new FeederHeaderSignatureCalculator(session))
                                 .check(status.is("${ExpectedResponseCode}"))
                                 .check(bodyJsonNode.is(elFileBodyAsJsonNode("${JsonResponseBodyStore}"))))

    val storeRequestPass = exec(http("${TestCase} - Document Store")
                                .post("/document")
                                .formParam("requestBody", ELFileBody("${JsonRequestBodyStore}"))
                                .formUpload("file", "${JsonRequestDocumentStore}")
                                .headers(dsrsStoreHeaders)
                                .signatureCalculator((session) => new FeederHeaderSignatureCalculator(session))
                                .check(status.is("${ExpectedResponseCode}"))
                                .check(jsonPath("$.error").notExists)
                                .check(jsonPath("$.dsrsIdentifier").saveAs("dsrsIdentifier")))

    val lookupRequest = exec(http("${TestCase} - Metadata Lookup")
                        .get("/metadata/${dsrsIdentifier}")
                        .headers(defaultHeaders)
                        .signatureCalculator((session) => new FeederHeaderSignatureCalculator(session))
                        .check(status.is("${ExpectedResponseCode}"))
                        .check(bodyJsonNode.is(elFileBodyAsJsonNode("${JsonResponseBodyLookup}"))))

    val retrievalRequest = exec(http("${TestCase} - Document Retrieval")
                           .get("/document/${dsrsIdentifier}")
                           .headers(defaultHeaders)
                           .signatureCalculator((session) => new FeederHeaderSignatureCalculator(session))
                           .check(status.is("${ExpectedResponseCode}"))
                           .check(jsonPath("$.documentUrl").optional.saveAs("documentUrl")))

    val getRequest = exec(http("${TestCase} - Document Get")
                           .get("${documentUrl}")
                           .headers(defaultHeaders)
                           .signatureCalculator((session) => new FeederHeaderSignatureCalculator(session))
                           .check(status.is("${ExpectedResponseCode}"))
                           .check(md5.is("${JsonRequestDocumentStore}")))

    scenario(userStory)
      .repeat(getRepeatTimes(feeder, repeatTimes)) {
        feed(feeder)
        .exec(
          doSwitch("${execKey}") (
            "Store" -> exec(storeRequestError),
            "Lookup" -> exec(storeRequestPass, lookupRequest),
            "Retrieval" -> exec(storeRequestPass, retrievalRequest, getRequest),
            "LookupError" -> exec(lookupRequest)
          )
        )
    }
  }
}