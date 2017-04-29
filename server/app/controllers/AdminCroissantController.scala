package controllers

import dao.CroissantDAO
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.i18n.{I18nSupport, MessagesApi}
import utils.{Mailer, Settings}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class AdminCroissantController(
  val settings: Settings,
  val messagesApi: MessagesApi,
  mailer: Mailer,
  croissantDAO: CroissantDAO
) extends Controller with I18nSupport {

  val newCroissantsForm = Form(tuple("from" -> email, "name" -> nonEmptyText, "subject" -> optional(text)))

  def addNewCroissant = AuthenticatedAction.async(parse.tolerantJson) { implicit request =>
    if (settings.Croissants.adminTrigrams.contains(request.trigram)) {
      newCroissantsForm.bindFromRequest().fold(
        err => Future.successful(BadRequest(err.errorsAsJson)),
        {
          case (from, name, subject) =>
            croissantDAO.addCroissant(from, name, subject)
              .map(_ => {
                Logger.info(s"Croissant added for : [${from} - ${name}]")
                Ok
              })
        }
      )
    } else {
      Future.successful(Forbidden)
    }
  }

}
