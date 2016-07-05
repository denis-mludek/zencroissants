package common

import javax.inject._

import play.api._

class Config @Inject() (conf: Configuration) {
  object Mail {
    val contact = conf.getString("mail.contact").getOrElse(sys.error("Missing contact email"))
    val all = conf.getString("mail.all").getOrElse(sys.error("Missing all email"))
    val mock = conf.getBoolean("mail.mock").getOrElse(false)
  }
  Mail  // force object to be loaded

  object Api {
    val secret = conf.getString("api.secret").getOrElse(sys.error("Missing api.secret"))
  }
  Api  // force object to be loaded

  object Ui {
    val host = conf.getString("ui.host").getOrElse(sys.error("Missing ui host url"))
  }
  Ui  // force object to be loaded

  object Croissants {
    val excludedEmails = {
      val str = conf.getString("croissants.excluded_emails").getOrElse(sys.error("Missing excluded Emails"))
      str.split(",").map(_.trim).toList
    }
    val includedDomains = {
      val str = conf.getString("croissants.included_domains").getOrElse(sys.error("Missing included domains"))
      str.split(",").map(_.trim).toList
    }
  }
  Croissants  // force object to be loaded

  object Oauth {
    val scopes = conf.getString("oauth.scopes").getOrElse(sys.error("Missing oauth.scopes"))
    val urlAuthorize = conf.getString("oauth.url.authorize").getOrElse(sys.error("Missing oauth.url.authorize"))
    val urlToken = conf.getString("oauth.url.token").getOrElse(sys.error("Missing oauth.url.token"))
    val urlUserinfos = conf.getString("oauth.url.userinfos").getOrElse(sys.error("Missing oauth.url.userinfos"))
    val urlRemovetoken = conf.getString("oauth.url.removetoken").getOrElse(sys.error("Missing oauth.url.removetoken"))
    val clientId = conf.getString("oauth.client.id").getOrElse(sys.error("Missing oauth.client.id"))
    val clientSecret = conf.getString("oauth.client.secret").getOrElse(sys.error("Missing oauth.client.secret"))
  }
  Oauth  // force object to be loaded
}
