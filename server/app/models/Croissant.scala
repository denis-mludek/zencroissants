package models

import java.time.{ZonedDateTime}

import play.api.libs.json.{Json, OFormat}

case class Croissant(
  id: String,
  victimId: String,
  creationDate: ZonedDateTime,
  doneDate: Option[ZonedDateTime],
  scheduleDate: Option[ZonedDateTime],
  status: Status,
  voters: Seq[String],
  email: String,
  name: String
) {
  def isDone = doneDate.isDefined
}

object Croissant {
  implicit val fmt: OFormat[Croissant] = Json.format[Croissant]
}
