package controllers

import models.LoggedRequest
import play.api.mvc.{ActionBuilder, Controller, Request, Result}

import scala.concurrent.Future

object AuthenticatedAction extends ActionBuilder[LoggedRequest] with Controller {
  def invokeBlock[A](request: Request[A], block: (LoggedRequest[A]) => Future[Result]) = {
    request.session.get("email") match {
      case Some(email) => block(LoggedRequest(email, request))
      case None => Future.successful(Redirect(routes.OauthController.login(request.uri)))
    }
  }
}