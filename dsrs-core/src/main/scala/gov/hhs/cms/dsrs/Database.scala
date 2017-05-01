package gov.hhs.cms.dsrs

import scala.concurrent.Future

/**
  * Represents a database with a blocking API
  */
trait Database {
  def executeQuery(query: String): String
}

/**
  * Represents a database with a non-blocking API
  */
trait AsyncDatabase {
  def executeQueryAsync(query: String): Future[String]
}
