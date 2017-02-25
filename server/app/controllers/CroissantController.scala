package controllers

import java.time.{ZoneOffset, ZonedDateTime}

import dao.CroissantDAO
import models.LoggedRequest
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json._
import utils.{Mailer, Settings}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future


class CroissantController(
  val settings: Settings,
  val messagesApi: MessagesApi,
  mailer: Mailer,
  croissantDAO: CroissantDAO
) extends Controller with I18nSupport {

  // if (config.Gmail.activated)
  //   gmailJob.schedule(None)

  def index = AuthenticatedAction.async { implicit request =>
    croissantDAO.findNotDone(croissantDAO.getUserIdFromEmail(request.email).getOrElse("")).flatMap {
      case croissants if croissants.isEmpty =>
        croissantDAO.listNotDone().map { list =>
          Ok(views.html.index(list.sortBy(_.creationDate.toInstant.toEpochMilli).reverse))
        }
      case croissants =>
        Future.successful(Redirect(routes.CroissantController.owned(croissants.head.id)))
    }
  }

  val newCroissantsForm = Form(tuple("from" -> email, "subject" -> optional(text), "secret" -> nonEmptyText))
  def newCroissant = Action.async(parse.tolerantJson) { implicit request =>
    newCroissantsForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(err.errorsAsJson)),
      {
        case (from, subject, settings.Api.secret) =>
          val email = from.trim
          croissantDAO.addCroissant(email, "", subject).map(_ => Ok)
        case _ => Future.successful(Forbidden)
      }
    )
  }

  def owned(id: String) = AuthenticatedAction.async { implicit request =>
    val victimId = croissantDAO.getUserIdFromEmail(request.email)
    croissantDAO.findById(id).map {
      case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
        Ok(views.html.victim.step1(victimId.get, croissant))
      case Some(croissant) => {
        Unauthorized(Json.obj(
          "error" -> "Unauthorized",
          "croissant" -> croissant.victimId,
          "victim" -> victimId
        ))
      }
      case None =>
        NotFound(Json.obj("error" -> "Croissant not found :-("))
    }
  }

  def schedule(id: String) = AuthenticatedAction.async { implicit request =>
    val victimId = croissantDAO.getUserIdFromEmail(request.email)
    croissantDAO.findById(id).flatMap {
      case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
        croissantDAO.findByDate().map { croissants =>
          Ok(views.html.victim.step2(croissants, croissant))
        }
      case Some(_) =>
        Future.successful(Unauthorized(Json.obj("error" -> "Unauthorized")))
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Croissant not found :-(")))
    }
  }

  val chooseForm = Form(
    "date" -> localDate("yyyy-MM-dd")
//       .verifying("Invalid Date", date => date.hourOfDay().withMaximumValue()
//                                              .minuteOfHour().withMaximumValue()
//                                              .secondOfMinute().withMaximumValue().isAfterNow
//                                              && date.plus(Period.months(2)).isBeforeNow
//      )
  )
  def choose(id: String) = AuthenticatedAction.async { implicit request =>
    chooseForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },{ date =>
        val victimId = getUserIdFromEmail(request.email)
        croissantDAO.findById(id).flatMap {
          case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
            val zonedDateTime = date.atStartOfDay(ZoneOffset.UTC)
            croissantDAO.chooseDate(id, zonedDateTime).map { _ =>
              Ok(views.html.victim.step3(victimId.get))
            }
          case Some(_) =>
            Future.successful(Unauthorized(Json.obj("error" -> "Unauthorized")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "Croissant not found :-(")))
        }
      }
    )
  }

  def confirmation(id: String) = AuthenticatedAction.async { implicit request =>
    croissantDAO.findById(id).map {
      case Some(croissant) if croissant.victimId == request.trigram && !croissant.isDone && croissant.scheduleDate.isDefined =>
        Ok(views.html.victim.step3(request.trigram))
      case Some(_) =>
        Forbidden(Json.obj("error" -> "Thats not yours :)"))
      case None =>
        NotFound(Json.obj("error" -> "Croissant not found :-("))
    }
  }

  def confirm(id: String) = AuthenticatedAction.async { implicit request =>
    croissantDAO.findById(id).flatMap {
      case Some(croissant) if croissant.victimId != request.trigram =>
        croissantDAO.vote(croissant, from = request.trigram).map { _ =>
          Ok(Json.obj("success" -> "Ton vote a été pris en compte !", "reload" -> true))
        }
      case Some(_) => Future.successful {
        Forbidden(Json.obj("error" -> "Ahah, tu pensais vraiment pouvoir voter pour toi?"))
      }
      case None => Future.successful {
        NotFound(Json.obj("error" -> "Croissant not found :-("))
      }
    }
  }

  val pressionFired = scala.collection.concurrent.TrieMap.empty[(String, String), ZonedDateTime]
  def pression(id: String) = AuthenticatedAction.async { request =>
    croissantDAO.findById(id).map {
      case Some(croissant) =>
        val key = (croissant.id, request.trigram)
        val now = ZonedDateTime.now
        pressionFired.get(key) match {
          case Some(date) => //if (now.toInstant.toEpochMilli - date.toInstant.toEpochMilli) < 1000*3600*24 =>
            play.api.Logger.debug(s"Not making pression on $id by ${request.trigram}")
            BadRequest(Json.obj("error" -> "Doucement, laisse lui du temps avant de re-voter."))
          case _ =>
            play.api.Logger.info(s"Make pression on $id by ${request.trigram}")
            mailer.pression(croissant.victimId, request.trigram, croissant.email)
            pressionFired += (key -> now)
            Ok(Json.obj("success" -> "Tu lui a bien mit la pression, cela va-t-il suffire ?"))
        }
      case None => NotFound(Json.obj("error" -> "Croissant not found :-("))
    }
  }

  private def getUserIdFromEmail(email: String): Option[String] = {
    val domains = settings.Croissants.includedDomains
    val excludedEmails = settings.Croissants.excludedEmails

    if (domains.exists(domain => email.endsWith(domain)) && !excludedEmails.contains(email)) {
      Some(email.split("@")(0))
    } else {
      None
    }
  }

}
