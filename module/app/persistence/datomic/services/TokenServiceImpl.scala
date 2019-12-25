package persistence.datomic.services

import java.util.UUID

import javax.inject.Inject
import persistence.datomic.{DatomicAuthService, TokenUser}

import scala.concurrent.{ExecutionContext, Future}

class TokenServiceImpl @Inject()(implicit datomicService: DatomicAuthService, ec: ExecutionContext) extends TokenService[TokenUser] {

  implicit val conn = datomicService.conn
  protected[this] val e = conn

  def create(token: TokenUser): Future[Option[TokenUser]] = {
    TokenUser.save(token).map(Some(_))
  }

  def retrieve(id: UUID): Future[Option[TokenUser]] = {
    TokenUser.findById(id)
  }

  def consume(id: UUID): Unit = {
    TokenUser.delete(id)(datomicService, ec)
  }
}
