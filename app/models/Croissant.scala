package models

import org.joda.time.DateTime
import play.api.libs.json.{JsResult, JsValue, _}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import enum.Enum
import utils.Repository

import scala.concurrent.Future

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

case class Croissant(
  id: String,
  victimId: String,
  creationDate: DateTime,
  doneDate: Option[DateTime],
  status: Status,
  voters: Seq[String]
)

object Croissant extends Repository[Croissant] {
  val collectionName: String = "croissants"
  implicit val format: OFormat[Croissant] = Json.format[Croissant] //.asInstanceOf[OFormat[Croissant]]

  def genId() = java.util.UUID.randomUUID.toString

  def add(userId: String)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    val croissant = Croissant(genId(), userId, DateTime.now(), None, Status.Pending, Seq())
    Croissant.save(croissant)
  }

  def chooseDate(id: String, date: DateTime)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    val query = Json.obj(
      "id" -> id
    )
    update(query, Json.obj(
      "$set" -> Json.obj(
        "doneDate" -> date
      )
    ))
  }

  def findById(id: String)(implicit reactiveMongoApi: ReactiveMongoApi) = findByOpt(Json.obj("id" -> id))

  def findNotDone(victimId: String)(implicit reactiveMongoApi: ReactiveMongoApi) = {
    findByOpt(Json.obj(
      "victimId" -> victimId,
      "doneDate" -> Json.obj(
        "$exists" -> false
      )
    ))
  }

  def findByDate(implicit reactiveMongoApi: ReactiveMongoApi) = {
    val beginDate = DateTime.now.dayOfMonth().withMinimumValue()
                      .hourOfDay().withMinimumValue()
                      .minuteOfHour().withMinimumValue()
                      .secondOfMinute().withMinimumValue()
    val query = Json.obj(
      "doneDate" -> Json.obj(
        "$gte" -> beginDate
      ),
      "doneDate" -> Json.obj(
        "$exists" -> true
      )
    )
    list(query)
  }

}
