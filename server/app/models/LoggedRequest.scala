package models

import play.api.mvc.{Request, WrappedRequest}

case class LoggedRequest[A](
  email: String,
  request: Request[A]
) extends WrappedRequest[A](request) {
  def trigram = email.slice(0, email.indexOf('@'))
}
