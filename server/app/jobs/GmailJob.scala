package jobs

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import dao.CroissantDAO
import models.Logging
import org.joda.time.DateTime
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.WSClient
import utils.{Mailer, Settings}

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class AccessToken(
  value: String,
  refreshAt: DateTime
)

class GmailJob(
  val ws: WSClient,
  val system: ActorSystem,
  val settings: Settings,
  mailer: Mailer,
  croissantDAO: CroissantDAO
) extends Logging {

  val refreshtoken = settings.Gmail.refreshtoken
  val clientId = settings.Gmail.clientId
  val clientSecret = settings.Gmail.clientSecret

  val fromRegex = "(.*)<(.*)>".r
  val receivedFromRegex = "from +.*.google.com\\b".r

  def schedule(accessToken: Option[AccessToken]): Unit = {
    system.scheduler.scheduleOnce(5 seconds) {
      val now = DateTime.now

      val accessTokenFtr = if (accessToken.isEmpty || now.isAfter(accessToken.get.refreshAt)) {
        refreshAccessToken()
      } else Future.successful(accessToken.get)

      val result = for {
        accessToken <- accessTokenFtr
        messages <- listUnreadGmailMessages(accessToken)
      } yield (accessToken, messages)

      result.onComplete {
        case Success((accessToken, messages)) =>
          messages.foreach { message =>
            for {
              from <- message.payload.headers.collect { case Header("From", value) => value }.headOption
              (name, email) <- from match {
                case fromRegex(name, email) => Some((name.trim, email.trim))
              }
            } yield {
              val receivedFromHeaders = message.payload.headers.filter(header => header.name == "Received" && header.value.trim.startsWith("from"))
              val headerValid = receivedFromHeaders.nonEmpty && receivedFromHeaders.forall { header =>
                receivedFromRegex.findFirstIn(header.value).isDefined
              }

              if (headerValid) {
                val subject = message.payload.headers.collect { case Header("Subject", value) => value }.headOption
                croissantDAO.addCroissant(email, name, subject)
              } else {
                logger.warn(s"Received invalid email with headers: ${message.payload.headers}")
              }
            }
          }
          schedule(Some(accessToken))
        case Failure(e) =>
          logger.error("Error during gmail job: ", e)
          schedule(accessToken)
      }
    }
    ()
  }

  case class GmailMessage(
    id: String,
    payload: Payload
  )
  case class Payload(
    headers: Seq[Header]
  )
  case class Header(
    name: String,
    value: String
  )
  implicit val headerReads: Reads[Header] = Json.reads[Header]
  implicit val payloadReads: Reads[Payload] = Json.reads[Payload]
  implicit val gmailMessagesReads: Reads[GmailMessage] = Json.reads[GmailMessage]
  private def listUnreadGmailMessages(accessToken: AccessToken): Future[Seq[GmailMessage]] = {
    val gmailMessagesBaseUrl = "https://www.googleapis.com/gmail/v1/users/me/messages"
    ws.url(gmailMessagesBaseUrl)
      .withQueryString("q" -> "is:unread")
      .withHeaders(
        "Authorization" -> s"Bearer ${accessToken.value}"
      )
      .get()
      .map {
        case response if response.status == 200 =>
          (response.json \ "messages" \\ "id").map(_.as[String])
        case response => throw new Exception(s"Could not get new messages ids, error: ${response.status} ${response.statusText}")
      }
      .flatMap { ids =>
        Future.traverse(ids) { id =>
          for {
            message <- {
              ws.url(s"$gmailMessagesBaseUrl/$id")
                .withQueryString("format" -> "metadata")
                .withHeaders(
                  "Authorization" -> s"Bearer ${accessToken.value}"
                )
                .get()
                .map {
                  case response if response.status == 200 =>
                    response.json.as[GmailMessage]
                  case response => throw new Exception(s"Could not get message $id details, error: ${response.status} ${response.statusText}")
                }
            }
            _ <- {
              ws.url(s"$gmailMessagesBaseUrl/$id/modify")
                .withHeaders(
                  "Authorization" -> s"Bearer ${accessToken.value}"
                )
                .post(Json.obj(
                  "removeLabelIds" -> Seq("UNREAD")
                ))
                .map {
                  case response if response.status == 200 => ()
                  case response => throw new Exception(s"Could not remove UNREAD label from message $id, error: ${response.status} ${response.statusText}")
                }
            }
          } yield message
        }
      }
  }

  private def refreshAccessToken(): Future[AccessToken] = {
    ws.url("https://www.googleapis.com/oauth2/v3/token")
      .post(Map(
        "client_id" -> Seq(clientId),
        "client_secret" -> Seq(clientSecret),
        "grant_type" -> Seq("refresh_token"),
        "refresh_token" -> Seq(refreshtoken)
      ))
      .map {
        case response if response.status == 200 =>
          val accessToken = (response.json \ "access_token").as[String]
          val expiresIn = (response.json \ "expires_in").as[Int]
          AccessToken(accessToken, DateTime.now.plus(expiresIn * 1000 / 2))
        case response => throw new Exception(s"Could not refresh access token, error: ${response.status} ${response.statusText}")
      }
  }

}