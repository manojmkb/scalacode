package gov.hhs.cms.dsrs.test.simulation.smoketest

import gov.hhs.cms.dsrs.test.simulation.common.DSRSBaseSimulation
import gov.hhs.cms.dsrs.test.simulation.component.DsrsComponentSimulation
import io.gatling.core.Predef._

/**
  * Created by armaun.mirshamsi on 1/17/2017.
  */
class SmokeTestSimulation extends
  DSRSBaseSimulation("ffm-gatling-env.dsrs.test.simulation.smoketest.SmokeTestSimulation"){

  // Smoke Test
  val smokeTestFeeder = jsonFile(feederFolder.concat("/smoketest-feeder.json")).queue

  val smokeTestList = List(
    DsrsComponentSimulation.postRequestJsonNodeDsrsScenario("Smoke Test",DsrsComponentSimulation.dsrsStoreHeaders, smokeTestFeeder)
      .inject(atOnceUsers(simulationAtOnceUsers)).protocols(httpConf)
  )

  setUp(smokeTestList).assertions(global.failedRequests.count.is(0))
}
