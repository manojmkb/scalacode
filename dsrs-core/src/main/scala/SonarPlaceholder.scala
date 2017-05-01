/**
  * Created by Charlie Davis on 2/16/17.
  *
  * Sonar is looking for any object in this package and failing when it cannot find one. (Sonar
  * is ignoring interfaces while doing this)
  */
object SonarPlaceholder {
  def hold(str: String): String = "Don't delete until something is implemented here or sonar will break"
}
