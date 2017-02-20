package dao

import java.time.ZonedDateTime

import models.{Croissant, Logging, Status}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{Json, OFormat}
import reactivemongo.api.commands.WriteResult
import utils.{Mailer, Settings}

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.ReactiveMongoApi

class CroissantDAO(
  val settings: Settings,
  mailer: Mailer,
  implicit val reactiveMongoApi: ReactiveMongoApi
)(implicit val format: OFormat[Croissant]) extends Repository[Croissant] with Logging {

  val collectionName = "croissants"

  def genId() = java.util.UUID.randomUUID.toString

  def getUserIdFromEmail(email: String): Option[String] = {
    val domains = settings.Croissants.includedDomains
    val excludedEmails = settings.Croissants.excludedEmails

    if (domains.exists(domain => email.endsWith(domain)) && !excludedEmails.contains(email)) {
      Some(email.split("@")(0))
    } else {
      None
    }
  }

  def add(userId: String, email: String, name: String): Future[WriteResult] = {
    val croissant = Croissant(genId(), userId, ZonedDateTime.now, None, None, Status.Pending, Nil, email, name)
    logger.info(s"Add croissant ${croissant.id}($userId)")
    save(croissant)
  }

  def addCroissant(email: String, name: String, subject: Option[String]): Future[Unit] = {
    getUserIdFromEmail(email) match {
      case Some(victimId) =>
        logger.debug(s"New croissants for : $email")
        add(victimId, email, name).map { _ =>
          mailer.victim(victimId, email)
          mailer.all(victimId, subject, settings.Ui.host)
          ()
        }
      case None =>
        Future.successful(Logger.debug(s"Mail ignored from : $email"))
    }
  }

  def chooseDate(id: String, date: DateTime): Future[WriteResult] = {
    val query = Json.obj(
      "id" -> id
    )
    update(query, Json.obj(
      "$set" -> Json.obj(
        "scheduleDate" -> date
      )
    ))
  }

  def findById(id: String) = findByOpt(Json.obj("id" -> id))

  def vote(croissant: Croissant, from: String) = {
    logger.info(s"User $from voted for croissant ${croissant.id}(${croissant.victimId})")
    update(
      Json.obj("id" -> croissant.id),
      Json.obj("$addToSet" -> Json.obj("voters" -> from))
    ).flatMap { _ =>
      if (croissant.voters.size >= settings.Croissants.nbVotersToDone && !croissant.isDone) {
        update(
          Json.obj(
            "id" -> croissant.id,
            "scheduleDate" -> Json.obj("$exists" -> false)
          ),
          Json.obj("$set" -> Json.obj("scheduleDate" -> DateTime.now))
        )
      }
      else Future.successful(())
    }
  }

  def listNotDone() = {
    list(Json.obj(
      "doneDate" -> Json.obj(
        "$exists" -> false
      )
    ))
  }

  def findNotDone(victimId: String) = {
    findByOpt(Json.obj(
      "victimId" -> victimId,
      "scheduleDate" -> Json.obj(
        "$exists" -> false
      )
    ))
  }

  def findByDate() = {
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
