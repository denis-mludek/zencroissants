package modules.mail

import javax.inject.Inject

import play.api.libs.mailer._
import java.io.File

import common.Config
import org.apache.commons.mail.EmailAttachment

class Mail @Inject() (config: Config, mailerClient: MailerClient) {

  def confirm(victimName: String, userName: String, to: String = config.Mail.all, toName: String = "All", subject : String = "Croissants") = {
    send(Email(
      subject = subject,
      from = "Zencroissants <"+ config.Mail.contact +">",
      to = Seq(toName + "<"+ to +">"),
      bodyHtml = Some(views.txt.email.confirmCroissants(victimName, userName).toString)
    ))
  }

  def all(victimName: String, mbMessage: Option[String], zencroissantURL: String) = {
    send(Email(
      subject = "Zencroissant a désigné sa nouvelle victime !",
      from = "Zencroissants <"+ config.Mail.contact +">",
      to = Seq("All <"+ config.Mail.all +">"),
      bodyHtml = Some(views.txt.email.zenall(victimName, mbMessage, zencroissantURL).toString)
    ))
  }

  def pression(victimName: String, userName: String, to: String) = {
    send(Email(
      subject = userName + " vient de te relancer. Tu vas devoir payer tes croissants rapidos.",
      from = "Zencroissants <"+ config.Mail.contact +">",
      to = Seq(victimName + " <"+ to +">"),
      bodyHtml = Some(views.txt.email.pression(victimName, userName).toString)
    ))
  }

  def victim(victimName: String, to: String) = {
    send(Email(
      subject = "Croissify !",
      from = "Zencroissants <"+ config.Mail.contact +">",
      to = Seq(victimName + " <"+ to +">"),
      bodyHtml = Some(views.txt.email.victim(victimName).toString)
    ))
  }

  def send(email: Email) = {
    if(config.Mail.mock) {
      println(email)
    } else {
      mailerClient.send(email)
    }
  }
}
