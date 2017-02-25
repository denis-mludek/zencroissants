package controllers

import play.api.mvc.{Action, Controller, Result}
import services.OauthProvider

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class OauthController(
  oauth: OauthProvider
) extends Controller {

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
