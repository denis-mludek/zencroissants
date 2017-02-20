package models

import play.api.libs.json._
import enum.Enum

sealed trait Status
object Status {
  case object Pending extends Status
  case object Done extends Status

  val statusEnum: Enum[Status] = Enum.derived[Status]

  implicit val statusJsonFormat: Format[Status] = new Format[Status] {
    override def reads(json: JsValue): JsResult[Status] = json.validate[String].flatMap { str =>
      statusEnum.decode(str) match {
        case Right(status) => JsSuccess(status)
        case Left(err) => JsError(s"Invalid status value. Valid values are : ${err.validValues}")
      }
    }
    override def writes(status: Status): JsValue = JsString(statusEnum.encode(status))
  }
}