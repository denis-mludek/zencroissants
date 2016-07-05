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

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Croissants @Inject()(
  val messagesApi: MessagesApi,
  val config: Config,
  val mailer: Mail)
  (implicit reactiveMongoApi: ReactiveMongoApi, ec: ExecutionContext) extends Controller with I18nSupport {

  case class AuthenticatedRequest[A](email: String, request: Request[A]) extends WrappedRequest[A](request)

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

  def index = AuthenticatedAction.async {
    Croissant.list.map { list =>
      Ok(views.html.index(list))
    }
  }

  private def getUserIdFromEmail(email: String): Option[String] = {
    val domains = config.Croissants.includedDomains
    val excludedEmails = config.Croissants.excludedEmails

    if (domains.exists(domain => email.endsWith(domain)) && !excludedEmails.contains(email)) {
      Some(email.split("@")(0))
    } else {
      None
    }
  }

}
