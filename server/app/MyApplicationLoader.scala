import dao.CroissantDAO
import jobs.GmailJob
import play.api._
import play.api.ApplicationLoader.Context
import router.Routes
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.libs.mailer._
import play.api.i18n.I18nComponents
import play.modules.reactivemongo.{ReactiveMongoApiFromContext, ReactiveMongoComponents}
import services.OauthProvider
import utils.{Mailer, Settings}


class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach(_.configure(context.environment))
    new MyComponents(context).application
  }
}

class MyComponents(context: Context) extends ReactiveMongoApiFromContext(context)
  with I18nComponents
  with AhcWSComponents
  with MailerComponents
  with ReactiveMongoComponents {

  implicit val ec = actorSystem.dispatcher

  val settings = new Settings(configuration)
  val mailer = new Mailer(settings, mailerClient)

  val croissantDAO = new CroissantDAO(settings, mailer, reactiveMongoApi)
  val oauthProvider = new OauthProvider(settings, wsClient)
  val gmailJob = new GmailJob(wsClient, actorSystem, settings, mailer, croissantDAO)

  lazy val router = new Routes(
    httpErrorHandler,
    new controllers.CroissantController(settings, messagesApi, mailer, croissantDAO),
    new controllers.OauthController(oauthProvider),
    new controllers.Assets(httpErrorHandler)
  )
}