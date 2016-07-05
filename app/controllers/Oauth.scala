package controllers

import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Result }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class Oauth @Inject()(
  val oauth: models.OauthProvider
)(implicit ec: ExecutionContext) extends Controller {

  val logger = play.api.Logger("extoauth")

  def login(url: String) = Action {
    oauth.getSecureAuthorizeUrl(url) match { case (url, stoken) =>
      Redirect(url).withSession("stoken" -> stoken)
    }
  }

  def logout(url: String) = Action {
    Redirect(url).withSession()
  }

  def callback() = Action.async { request =>
    val stoken = request.session.get("stoken")
    oauth.parseStateFromRequest(request) match {
      case Some(state) =>
        oauth.getProviderUserEmail(stoken, state, request).map {
          case Some(userEmail) => Redirect(state.redirectUrl).withSession("email" -> userEmail)
          case None => redirectWithError(state.redirectUrl, "Authentication failed")
        }
      case None => Future.successful(redirectWithError("/", "Authentication failed"))
    }
  }

  private def redirectWithError(url: String, error: String): Result = {
    val sep = oauth.urlSep(url)
    Redirect(url + sep + oauth.encodeParams("error" -> error))
  }

}
