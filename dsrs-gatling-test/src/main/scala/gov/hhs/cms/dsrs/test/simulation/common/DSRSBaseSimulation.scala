package gov.hhs.cms.dsrs.test.simulation.common

import gov.hhs.cms.arch.test.gatling.{BaseSimulation, PredefinedScenarios}
import io.gatling.http.Predef._
import io.gatling.http.{HeaderNames, HeaderValues}

/**
  * Created by afs on 1/12/16.
  */
class DSRSBaseSimulation(T: String) extends BaseSimulation(T) with PredefinedScenarios {

  val simulationBaseUrl = gatlingSimulationConfiguration.getString(simulationPropertyKey("simulationBaseUrl"))

  val simulationAtOnceUsers = gatlingSimulationConfiguration.getInt(simulationPropertyKey("simulationAtOnceUsers"))

  val feederFolder = gatlingSimulationConfiguration.getString(simulationPropertyKey("feederFolder"))

  val clientSystemId = gatlingSimulationConfiguration.getString(simulationPropertyKey("clientSystemId"))

  val roleId = gatlingSimulationConfiguration.getString(simulationPropertyKey("roleId"))

  val userId = gatlingSimulationConfiguration.getString(simulationPropertyKey("userId"))

  val httpConf = http
    .baseURL(simulationBaseUrl)

  val defaultHeaders = Map(
    "userId" -> userId,
    "clientSystemId" -> clientSystemId,
    "roleId" -> roleId
  )
}
