package gov.hhs.cms.dsrs.test.simulation.regression

import gov.hhs.cms.arch.test.gatling.BaseSimulation
import gov.hhs.cms.dsrs.test.simulation.component._
import io.gatling.core.Predef._

/**
  * Created by afs on 3/1/16.
  */
class RegressionSimulation extends
  BaseSimulation("ffm-gatling-env.dsrs.test.simulation.regression.RegressionSimulation"){

  val dsrsSimulation = new DsrsComponentSimulation

  // Creating the regression scenario list by
  // combining the scenario lists from targeted component simulations
  val simulationScenariosList = List.concat(
    dsrsSimulation.simulationScenariosList
  )

  setUp(simulationScenariosList).assertions(global.failedRequests.count.is(0))
}