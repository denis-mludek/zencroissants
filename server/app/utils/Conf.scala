package utils

import java.time.LocalDate

import scala.concurrent.duration.FiniteDuration
import scala.collection.JavaConversions._
import java.time.format.DateTimeFormatter
import play.api.Configuration

class Conf(
  conf: Configuration
) {

  def requiredString(key: String) = requiredValue(optionalString, key)
  def requiredStringList(key: String) = requiredValue(optionalStringList, key)
  def requiredStringMap(key: String) = requiredValue(optionalStringMap, key)
  def requiredBoolean(key: String) = requiredValue(optionalBoolean, key)
  def requiredMs(key: String) = requiredValue(optionalMs, key)
  def requiredInt(key: String) = requiredValue(optionalInt, key)
  def requiredDate(key: String) = validateDate(requiredValue(optionalString, key))
  def requiredDuration(key: String) = requiredValue(optionalDuration, key)
  def requiredLong(key: String) = requiredValue(optionalLong, key)

  def optionalString(key: String) = conf.getString(key)
  def optionalStringList(key: String) = conf.getStringList(key)
  def optionalStringMap(key: String) = conf.getObject(key).map(obj => obj.map {
    case (key, value) => (key, value.unwrapped.toString)
  }.toMap)
  def optionalBoolean(key: String) = conf.getBoolean(key)
  def optionalMs(key: String) = conf.getMilliseconds(key)
  def optionalInt(key: String) = conf.getInt(key)
  def optionalDuration(key: String) = optionalMs(key).map(ms => FiniteDuration(ms, "millis"))
  def optionalLong(key: String) = conf.getLong(key)

  private def requiredValue[A](getter: String => Option[A], key: String): A =
    getter(key).getOrElse(sys.error(s"Missing config key: $key"))

  private def validateDate(mayBeDate : String ): String = {
    try {
      val format = DateTimeFormatter.ofPattern("d/MM/uuuu")
      LocalDate.parse(mayBeDate, format)
      mayBeDate
    } catch {
      case _: Throwable => {
        sys.error(s"Format date error : ${mayBeDate}")
      }
    }
  }
}