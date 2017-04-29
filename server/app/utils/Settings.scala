package utils

import play.api._

class Settings(
  conf: Configuration
) {
  val confUtils = new Conf(conf)
  import confUtils._

  object Mail {
    val contact = requiredString("mail.contact")
    val all = requiredString("mail.all")
    val mock = optionalBoolean("mail.mock").getOrElse(false)
  }

  object Api {
    val secret = requiredString("api.secret")
  }

  object Ui {
    val host = requiredString("ui.host")
  }

  object Croissants {
    val nbVotersToDone = requiredInt("croissants.nb_voters_to_done")

    val excludedEmails = {
      val str = requiredString("croissants.excluded_emails")
      str.split(",").map(_.trim).toList
    }

    val includedDomains = {
      val str = requiredString("croissants.included_domains")
      str.split(",").map(_.trim).toList
    }

    val adminTrigrams = {
      val str = requiredString("croissants.admin_trigrams")
      str.split(",").map(_.trim).toList
    }
  }

  object mongodb {
    val uri = requiredString("mongodb.uri")
  }

  object Oauth {
    val scopes = requiredString("oauth.scopes")
    val urlAuthorize = requiredString("oauth.url.authorize")
    val urlToken = requiredString("oauth.url.token")
    val urlUserinfos = requiredString("oauth.url.userinfos")
    val urlRemovetoken = requiredString("oauth.url.removetoken")
    val clientId = requiredString("oauth.client.id")
    val clientSecret = requiredString("oauth.client.secret")
  }

  object Gmail {
    val activated = optionalBoolean("gmail.activated").getOrElse(true)
    val refreshtoken = requiredString("gmail.refreshtoken")
    val clientId = requiredString("gmail.client.id")
    val clientSecret = requiredString("gmail.client.secret")
  }
}
