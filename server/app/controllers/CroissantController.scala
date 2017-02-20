package controllers

import java.time.ZonedDateTime

import dao.{CroissantDAO}
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, ActionBuilder, Controller, Request, Result, WrappedRequest}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
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

  case class AuthenticatedRequest[A](email: String, request: Request[A]) extends WrappedRequest[A](request) {
    def trigram = email.slice(0, email.indexOf('@'))
  }

  object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
      request.session.get("email") match {
        case Some(email) => block(AuthenticatedRequest(email, request))
        case None => Future.successful(Redirect(routes.OauthController.login(request.uri)))
      }
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
    println(request.email)
    val victimId = croissantDAO.getUserIdFromEmail(request.email)
    croissantDAO.findById(id).map {
      case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
        Ok(views.html.step1(victimId.get, croissant))
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
        croissantDAO.findByDate.map { croissants =>
          Ok(views.html.step2(croissants, croissant))
        }
      case Some(croissant) =>
        Future.successful(Unauthorized(Json.obj("error" -> "Unauthorized")))
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Croissant not found :-(")))
    }
  }

  val chooseForm = Form(
    "date" -> jodaDate("yyyy-MM-dd")
      // .verifying("Invalid Date", date => date.hourOfDay().withMaximumValue()
      //                                        .minuteOfHour().withMaximumValue()
      //                                        .secondOfMinute().withMaximumValue().isAfterNow
      //                                        && date.plus(Period.months(2)).isBeforeNow
      //)
  )
  def choose(id: String) = AuthenticatedAction.async { implicit request =>
    chooseForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      }, { case date =>
        val victimId = getUserIdFromEmail(request.email)
        croissantDAO.findById(id).flatMap {
          case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
            croissantDAO.chooseDate(id, date).map { result =>
              Ok(views.html.step3(victimId.get))
            }
          case Some(croissant) =>
            Future.successful(Unauthorized(Json.obj("error" -> "Unauthorized")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "Croissant not found :-(")))
        }
      }
    )
  }

  def confirm(id: String) = AuthenticatedAction.async { implicit request =>
    croissantDAO.findById(id).flatMap {
      case Some(croissant) if croissant.victimId != request.trigram =>
        croissantDAO.vote(croissant, from = request.trigram).map { _ =>
          Ok(Json.obj("success" -> "Croissant confirmed", "reload" -> true))
        }
      case Some(croissant) => Future.successful {
        Forbidden(Json.obj("error" -> "You can't vote for yourself (smart ass)"))
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
          case Some(date) if (now.toInstant.toEpochMilli - date.toInstant.toEpochMilli) < 1000*3600*24 =>
            play.api.Logger.debug(s"Not making pression on $id by ${request.trigram}")
            BadRequest(Json.obj("error" -> "Wait a bit before 2 pression sessions"))
          case _ =>
            play.api.Logger.info(s"Make pression on $id by ${request.trigram}")
            mailer.pression(croissant.victimId, request.trigram, croissant.email)
            pressionFired += (key -> now)
            Ok(Json.obj("success" -> "Pression on croissant FIRED"))
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
