package services

import models.Logging
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.RequestHeader
import utils.Settings

import scala.concurrent.Future
import scala.util.Success
import play.api.libs.concurrent.Execution.Implicits._

class OauthProvider(
  val settings: Settings,
  val ws: WSClient
) extends Logging {

  val scopes = settings.Oauth.scopes

  val authorizeUrl = settings.Oauth.urlAuthorize
  val tokenUrl = settings.Oauth.urlToken
  val userinfosUrl = settings.Oauth.urlUserinfos
  val removetokenUrl = settings.Oauth.urlRemovetoken
  val clientId = settings.Oauth.clientId
  val clientSecret = settings.Oauth.clientSecret

  def checkJsonResponse[A](desc: String, reads: Reads[A])(response: WSResponse) = {
    if (response.status == 200) {
      reads.reads(response.json).fold(
        errors => {
          logger.error(s"Could not $desc: ${response.json} (${JsError.toJson(errors)})")
          None
        },
        value => Some(value)
      )
    }
    else {
      logger.error(s"Could not $desc: ${response.json} (${response.status})")
      None
    }
  }

  def getAccessTokenFromCode(code: String): Future[Option[String]] = {
    logger.debug(s"Get access token from code (code=$code)")
    val params = Map(
      "code" -> code,
      "redirect_uri" -> callbackUrl(),
      "grant_type" -> "authorization_code",
      "client_id" -> clientId,
      "client_secret" -> clientSecret
    ).map { case (k, v) => (k, Seq(v)) }
    ws.url(tokenUrl)
      .post(params)
      .map {
        checkJsonResponse("get the access_token", (JsPath \ "access_token").read[String])
      }
  }

  def userEmailReads: Reads[String] = {
    import play.api.libs.functional.syntax._

    (JsPath \ "emails").read[Seq[JsValue]].flatMap { jsvalues =>
      Reads { _ =>
        jsvalues.map { jsvalue =>
          (
            (JsPath \ "value").read[String] ~
            (JsPath \ "type").read[String]
          ).tupled.reads(jsvalue).fold(
            _ => None,
            {
              case (email, "account") => Some(email)
              case _ => None
            }
          )
        }.flatten.headOption match {
          case Some(email) => JsSuccess(email)
          case None => JsError(s"Missing account't email ($jsvalues)")
        }
      }
    }
  }

  def getProviderUserEmailFromAccessToken(accessToken: String): Future[Option[String]] = {
    logger.debug(s"Get provider user id from access token (accessToken=$accessToken)")
    ws.url(userinfosUrl)
      .withHeaders("Authorization" -> s"Bearer $accessToken")
      .get
      .map {
        checkJsonResponse("get the id", userEmailReads)
      }
  }

  def deleteAccessToken(accessToken: String): Future[Unit] = {
    logger.debug(s"Delete access token (accessToken=$accessToken)")
    ws.url(removetokenUrl)
      .withQueryString("token" -> accessToken)
      .delete
      .map(_ => ())
  }

  def getSecureAuthorizeUrl(redirectUrl: String): (String, String) = {
    val sep = urlSep(authorizeUrl)
    val stoken = generateSecureToken()
    val state = buildState(State(stoken, redirectUrl))

    val url = authorizeUrl + sep + encodeParams(
      "response_type" -> "code",
      "client_id" -> clientId,
      "redirect_uri" -> callbackUrl(),
      "scope" -> scopes,
      "state" -> state
    )
    (url, stoken)
  }


  def callbackUrl() = {
    settings.Ui.host + controllers.routes.OauthController.callback().url
  }

  case class State(stoken: String, redirectUrl: String)

  def urlSep(url: String) = if (url.indexOf('?') < 0) '?' else '&'
  def encodeParams(params: (String, String)*): String = {
    params.map { case (key, value) =>
      key + "=" + java.net.URLEncoder.encode(value, "UTF-8")
    }.mkString("&")
  }
  def decodeParams(params: String): Map[String, String] = {
    params.split("&").map { keyvalue =>
      val split = keyvalue.split("=")
      split(0) -> java.net.URLDecoder.decode(split.lift(1).getOrElse(""), "UTF-8")
    }.toMap
  }

  def generateSecureToken() = scala.util.Random.alphanumeric.take(30).mkString("")

  def buildState(state: State): String = {
    encodeParams(
      "stoken" -> state.stoken,
      "redirectUrl" -> state.redirectUrl
    )
  }
  def parseState(encoded: String): Option[State] = {
    val decoded = decodeParams(encoded)
    for {
      stoken <- decoded.get("stoken")
      redirectUrl <- decoded.get("redirectUrl")
    } yield State(stoken, redirectUrl)
  }
  def parseStateFromRequest(request: RequestHeader): Option[State] = {
    request.getQueryString("state").flatMap(parseState)
  }

  def getProviderUserEmail(stoken: Option[String], state: State, request: RequestHeader): Future[Option[String]] = {
    if (stoken.exists(_ == state.stoken)) {
        request.getQueryString("code") match {
          case Some(code) =>
            getAccessTokenFromCode(code).flatMap {
              case Some(accessToken) =>
                getProviderUserEmailFromAccessToken(accessToken).andThen {
                  case _ => deleteAccessToken(accessToken)
                }
              case None => Future.successful(None)
            }.andThen {
              case Success(Some(providerUserEmail)) =>
                logger.debug(s"Get provider's user (code=$code, providerUserEmail=$providerUserEmail)")
              case Success(None) =>
                logger.debug(s"Can't get provider's user ($code)")
            }
          case code =>
            logger.debug(s"Can't get provider's user: invalid request (code=$code)")
            Future.successful(None)
        }
    }
    else {
      logger.debug(s"Can't get provider's user: Invalid stoken (stoken=$stoken, state=$state)")
      Future.successful(None)
    }
  }

}
