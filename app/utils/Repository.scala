package utils

import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Repository[T]  {

  val collectionName: String

  implicit val format: OFormat[T]

  def collection()(implicit reactiveMongoApi: ReactiveMongoApi) = reactiveMongoApi.db[JSONCollection](collectionName)

  def save(doc: T)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    collection.insert(doc)
  }

  def update(selector: JsObject, update: JsObject)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    collection.update(selector, update, upsert = false)
  }

  def upsert(selector: JsObject, doc: T)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    collection.update(selector, Json.obj("$set" -> doc), upsert = true)
  }

  def remove(query: JsObject)(implicit reactiveMongoApi: ReactiveMongoApi): Future[WriteResult] = {
    collection.remove(query)
  }

  def list(query: JsObject = Json.obj())(implicit reactiveMongoApi: ReactiveMongoApi): Future[Seq[T]] = {
    collection.find(query).cursor[T]().collect[Seq]()
  }

  def list(implicit reactiveMongoApi: ReactiveMongoApi): Future[Seq[T]] = list()

  def findByOpt(query: JsObject)(implicit reactiveMongoApi: ReactiveMongoApi): Future[Option[T]] = {
    collection.find(query).one[T]
  }

}
