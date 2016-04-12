package models

import java.time.{ LocalDateTime, ZoneId }
import java.util.Date

import datomic.Peer
import datomic.Util._
import datomisca.{ Attribute, Cardinality, EntityReader, Namespace, PartialAddEntityWriter, SchemaType, Unique }
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import play.api.Logger
import utils.persistence.datomic.DatomicService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class TokenUser(id: Long = -1, email: String, expirationTime: LocalDateTime = LocalDateTime.now().plusHours(24L), isSignUp: Boolean = false) extends Token {
  def isExpired: Boolean = expirationTime.isBefore(LocalDateTime.now())
}

object TokenUser extends DB[TokenUser] {

  object Schema {

    object ns {
      val tokenUser = new Namespace("tokenUser")
    }

    // Attributes
    val email = Attribute(ns.tokenUser / "email", SchemaType.string, Cardinality.one).withUnique(Unique.identity).withDoc("email address")
    val expirationTime = Attribute(ns.tokenUser / "expirationTime", SchemaType.instant, Cardinality.one).withDoc("time the link will expire")
    val isSignUp = Attribute(ns.tokenUser / "isSignUp", SchemaType.boolean, Cardinality.one).withDoc("true when sign up")

    val schema = Seq(
      email, expirationTime, isSignUp
    )

  }

  implicit val dateToLocalDateTime: Date => LocalDateTime = (date: Date) => LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault())
  implicit val localDateTimeToDate: LocalDateTime => Date = (ldt: LocalDateTime) => Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant)

  implicit val reader: EntityReader[TokenUser] = (
    ID.read[Long] and
    Schema.email.read[String] and
    Schema.expirationTime.read[Date].map(dateToLocalDateTime) and
    Schema.isSignUp.read[Boolean]
  )(TokenUser.apply _)

  implicit val writer: PartialAddEntityWriter[TokenUser] = (
    ID.write[Long] and
    Schema.email.write[String] and
    Schema.expirationTime.write[Date].contramap(localDateTimeToDate) and
    Schema.isSignUp.write[Boolean]
  )(unlift(TokenUser.unapply))

  private val hoursTillExpiry = 24L

  def findById(id: Long)(implicit conn: datomisca.Connection): Future[Option[TokenUser]] = {
    Future.successful(TokenUser.find(id))
  }

  def save(tokenUser: TokenUser)(implicit conn: datomisca.Connection): Future[TokenUser] = {

    val tokenUserFact = DatomicMapping.toEntity(DId(Partition.USER))(tokenUser)

    for {
      tx <- Datomic.transact(tokenUserFact)
    } yield TokenUser.get(tx.resolve(tokenUserFact))

  }

  def delete(id: Long)(implicit datomicService: DatomicService): Unit = {
    implicit val conn = datomicService.conn
    TokenUser.retractEntity(id)
    // Note that excision has no effect on in memory test db
    Peer.connect(datomicService.connectionUrl("prod")).transact(datomic.Util.list(datomic.Util.list(s"[{:db/id #db/id[db.part/user], :db/excise $id }]]")))
  }
}
