package persistence.datomic

import java.time.{LocalDateTime, ZoneId}
import java.util.{Date, UUID}

import datomic.Peer
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import scala.concurrent.{ExecutionContext, Future}

case class TokenUser(id: UUID = UUID.randomUUID(), email: String, expirationTime: LocalDateTime = LocalDateTime.now().plusHours(24L * 14), isSignUp: Boolean = false) extends Token {
  def isExpired: Boolean = expirationTime.isBefore(LocalDateTime.now())
}

object TokenUser extends DB[TokenUser] {

  object Schema {

    object ns {
      val tokenUser = new Namespace("tokenUser")
    }

    // Attributes
    val id = Attribute(ns.tokenUser / "id", SchemaType.uuid, Cardinality.one).withUnique(Unique.identity).withDoc("random token")
    val email = Attribute(ns.tokenUser / "email", SchemaType.string, Cardinality.one).withUnique(Unique.identity).withDoc("email address (latest action upsert)")
    val expirationTime = Attribute(ns.tokenUser / "expirationTime", SchemaType.instant, Cardinality.one).withDoc("time the link will expire")
    val isSignUp = Attribute(ns.tokenUser / "isSignUp", SchemaType.boolean, Cardinality.one).withDoc("true when sign up")

    val schema = Seq(
      id, email, expirationTime, isSignUp)

  }

  implicit val dateToLocalDateTime: Date => LocalDateTime = (date: Date) => LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault())
  implicit val localDateTimeToDate: LocalDateTime => Date = (ldt: LocalDateTime) => Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant)

  implicit val reader: EntityReader[TokenUser] = (
    Schema.id.read[UUID] and
      Schema.email.read[String] and
      Schema.expirationTime.read[Date].map(dateToLocalDateTime) and
      Schema.isSignUp.read[Boolean]) (TokenUser.apply _)

  implicit val writer: PartialAddEntityWriter[TokenUser] = (
    Schema.id.write[UUID] and
      Schema.email.write[String] and
      Schema.expirationTime.write[Date].contramap(localDateTimeToDate) and
      Schema.isSignUp.write[Boolean]) (unlift(TokenUser.unapply))

  private val hoursTillExpiry = 24L

  def findById(id: UUID)(implicit conn: datomisca.Connection): Future[Option[TokenUser]] = {
    Future.successful(TokenUser.find(LookupRef(TokenUser.Schema.id, id)))
  }

  def findAll()(implicit conn: Connection): Seq[TokenUser] = {
    val queryAll = Query(""" [ :find ?a :where [?a :tokenUser/email] ] """)
    list(Datomic.q(queryAll, Datomic.database))
  }

  def findByEmail(email: String)(implicit conn: Connection): Seq[TokenUser] = {
    val query = Query(""" [ :find ?a :in $ ?email :where [?a :tokenUser/email ?email] ] """)
    list(Datomic.q(query, Datomic.database, email))
  }

  def save(tokenUser: TokenUser)(implicit conn: datomisca.Connection, ec: ExecutionContext): Future[TokenUser] = {

    val tokenUserFact = DatomicMapping.toEntity(DId(Partition.USER))(tokenUser)

    for {
      tx <- Datomic.transact(tokenUserFact)
    } yield TokenUser.get(tx.resolve(tokenUserFact))

  }

  def delete(id: UUID)(implicit datomicService: DatomicAuthService, ec: ExecutionContext): Unit = {
    implicit val conn = datomicService.conn
    TokenUser.retractEntity(LookupRef(TokenUser.Schema.id, id))
    // Note that excision has no effect on in memory test db
    Peer.connect(datomicService.connectionUrl("prod")).transact(datomic.Util.list(datomic.Util.list(s"[{:db/id #db/id[db.part/user], :db/excise $id }]]")))
  }
}
