package models.services

import javax.inject.Inject

import models.TokenUser
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import utils.persistence.datomic.DatomicService

import scala.concurrent.Future

class TokenServiceImpl @Inject() (datomicService: DatomicService) extends TokenService[TokenUser] {

  implicit val conn = datomicService.conn
  protected[this] val e = conn

  def create(token: TokenUser): Future[Option[TokenUser]] = {
    TokenUser.save(token).map(Some(_))
  }
  def retrieve(id: String): Future[Option[TokenUser]] = {
    TokenUser.findById(id.toLong)
  }
  def consume(id: String): Unit = {
    TokenUser.delete(id.toLong)(datomicService)
  }
}
