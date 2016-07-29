package controllers

import javax.inject.{Inject, Singleton}

import common.Config
import models.Croissant
import modules.mail.Mail
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, ActionBuilder, Controller, Request, Result, WrappedRequest}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import org.joda.time._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Croissants @Inject()(
  val messagesApi: MessagesApi,
  val config: Config,
  val mailer: Mail)
  (implicit reactiveMongoApi: ReactiveMongoApi, ec: ExecutionContext) extends Controller with I18nSupport {

  case class AuthenticatedRequest[A](email: String, request: Request[A]) extends WrappedRequest[A](request) {
    def trigram = email.slice(0, email.indexOf('@'))
  }

  object AuthenticatedAction extends ActionBuilder[AuthenticatedRequest] {
    def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {
      request.session.get("email") match {
        case Some(email) => block(AuthenticatedRequest(email, request))
        case None => Future.successful(Redirect(routes.Oauth.login(request.uri)))
      }
    }
  }

  val newCroissantsForm = Form(tuple("from" -> email, "subject" -> text, "secret" -> nonEmptyText))
  def newCroissant = Action.async(parse.tolerantJson) { implicit request =>
    newCroissantsForm.bindFromRequest().fold(
      err => Future.successful(BadRequest(err.errorsAsJson)),
      {
        case (from, subject, config.Api.secret) =>
          val email = from.trim
          getUserIdFromEmail(email) match {
            case Some(victimId) =>
              Logger.debug(s"New croissants for : $email")
              val mbMessage: Option[String] =
                if(subject == null) {
                  Some(subject)
                }else{
                  None
                }
              Croissant.add(victimId).map { _ =>
                mailer.victim(victimId, email)
                mailer.all(victimId, mbMessage, config.Ui.host)
                Ok
              }
            case None =>
              Logger.debug(s"Mail ignored from : $email")
              Future.successful(Ok)
          }
        case _ => Future.successful(Forbidden)
      }
    )
  }

  def index = AuthenticatedAction.async { implicit request =>
    Croissant.findNotDone(getUserIdFromEmail(request.email).getOrElse("")).flatMap {
      case croissants if croissants.isEmpty =>
        Croissant.list.map { list =>
          Ok(views.html.index(list))
        }
      case croissants =>
        Future.successful(Redirect(routes.Croissants.owned(croissants.head.id)))
    }
  }

  def owned(id: String) = AuthenticatedAction.async { implicit request =>
    println("toto", request.email)
    val victimId = getUserIdFromEmail(request.email)

    Croissant.findById(id).map {
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
    val victimId = getUserIdFromEmail(request.email)
    Croissant.findById(id).flatMap {
      case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
        Croissant.findByDate.map { croissants =>
          Ok(views.html.step2(croissants))
        }
      case Some(croissant) =>
        Future.successful(Unauthorized(Json.obj("error" -> "Unauthorized")))
      case None =>
        Future.successful(NotFound(Json.obj("error" -> "Croissant not found :-(")))
    }
  }

  val chooseForm = Form(
    "date" -> jodaDate("yyyy-MM-dd")
  )
  def choose(id: String) = AuthenticatedAction.async { implicit request =>
    chooseForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      }, { case date =>
        val victimId = getUserIdFromEmail(request.email)
        Croissant.findById(id).flatMap {
          case Some(croissant) if victimId.isDefined && croissant.victimId == victimId.get =>
            Croissant.chooseDate(id, date).map { result =>
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
    Croissant.findById(id).map {
      case Some(croissant) =>
        println(s"Confirm $id by ${request.trigram}")
        // Croissant.vote(id)
        Ok(Json.obj("success" -> "Croissant confirmed"))
      case None => NotFound(Json.obj("error" -> "Croissant not found :-("))
    }
  }

  def pression(id: String) = AuthenticatedAction.async { request =>
    Croissant.findById(id).map {
      case Some(croissant) =>
        println(s"Make pression on $id by ${request.trigram}")
        // Croissant.pression(id)
        Ok(Json.obj("success" -> "Pression on croissant FIRED"))
      case None => NotFound(Json.obj("error" -> "Croissant not found :-("))
    }
  }

  private def getUserIdFromEmail(email: String): Option[String] = {
    val domains = config.Croissants.includedDomains
    val excludedEmails = config.Croissants.excludedEmails

    Some(email.split("@")(0))

    // if (domains.exists(domain => email.endsWith(domain)) && !excludedEmails.contains(email)) {
    //   Some(email.split("@")(0))
    // } else {
    //   None
    // }
  }

}
