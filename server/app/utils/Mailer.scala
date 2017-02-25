package utils

import play.api.libs.mailer.{Email, MailerClient}

class Mailer(
  settings: Settings,
  mailerClient: MailerClient
) {

  def confirm(victimName: String, userName: String, to: String = settings.Mail.all, toName: String = "All", subject : String = "Croissants") = {
    send(Email(
      subject = subject,
      from = "Zencroissants <"+ settings.Mail.contact +">",
      to = Seq(toName + "<"+ to +">"),
      bodyHtml = Some(views.html.email.confirmCroissants(victimName, userName).toString)
    ))
  }

  def all(victimName: String, mbMessage: Option[String], zencroissantURL: String) = {
    send(Email(
      subject = "Zencroissant a désigné sa nouvelle victime !",
      from = "Zencroissants <"+ settings.Mail.contact +">",
      to = Seq("All <"+ settings.Mail.all +">"),
      bodyHtml = Some(views.html.email.zenall(victimName, mbMessage, zencroissantURL).toString)
    ))
  }

  def pression(victimName: String, userName: String, to: String) = {
    send(Email(
      subject = userName + " vient de te relancer. Tu vas devoir payer tes croissants rapidos.",
      from = "Zencroissants <"+ settings.Mail.contact +">",
      to = Seq(victimName + " <"+ to +">"),
      bodyHtml = Some(views.html.email.pression(victimName, userName).toString)
    ))
  }

  def victim(victimName: String, to: String) = {
    send(Email(
      subject = "Croissify !",
      from = "Zencroissants <"+ settings.Mail.contact +">",
      to = Seq(victimName + " <"+ to +">"),
      bodyHtml = Some(views.html.email.victim(victimName).toString)
    ))
  }

  def send(email: Email) = {
    if(settings.Mail.mock) {
      println(email)
    } else {
      mailerClient.send(email)
    }
  }
}
