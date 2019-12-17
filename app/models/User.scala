package models

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import persistence.datomic.daos.LoginInfoImpl

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

/**
 * The user object.
 *
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName  Maybe the last name of the authenticated user.
 * @param fullName  Maybe the full name of the authenticated user.
 * @param email     Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 */
case class User(
  id: Long = -1,
  email: String = "",
  firstName: Option[String] = None,
  lastName: Option[String] = None,
  fullName: Option[String] = None,
  avatarURL: Option[String] = None,
  role: Role = Role.Member) extends Identity {

  final def hasRole(checkRole: Role): Boolean = role == checkRole

  final def isAdmin(checkRole: Role): Boolean = role == Role.Administrator

}

sealed abstract class Role(val name: String) {
  override def toString: String = name
}

object Role {

  case object Administrator extends Role("administrator")

  case object Member extends Role("member")

  val all: Set[Role] = Set(Administrator, Member)

  def apply(name: String): Role = {
    all.find(s => s.name == name) match {
      case Some(role) => role
      case None => throw new IllegalArgumentException(s"Invalid Role: $name")
    }
  }

}

object User extends DB[User] {

  object Schema {

    object ns {
      val user = new Namespace("user") {
        val role = Namespace("user.role")
      }
    }

    // Attributes
    val email = Attribute(ns.user / "email", SchemaType.string, Cardinality.one).withUnique(Unique.identity).withDoc("email address")
    val firstName = Attribute(ns.user / "firstName", SchemaType.string, Cardinality.one).withDoc("first name")
    val lastName = Attribute(ns.user / "lastName", SchemaType.string, Cardinality.one).withDoc("last name")
    val fullName = Attribute(ns.user / "fullName", SchemaType.string, Cardinality.one).withDoc("full name")
    val avatarURL = Attribute(ns.user / "avatarURL", SchemaType.string, Cardinality.one).withDoc("avatar url")
    val role = Attribute(ns.user / "role", SchemaType.ref, Cardinality.one).withDoc("The level of permission")

    val loginInfo = Attribute(ns.user / "loginInfo", SchemaType.ref, Cardinality.many).withIsComponent(true).withDoc("loginInfo for the user")

    // Role enumerated values
    val administrator = AddIdent(ns.user.role / Role.Administrator.toString)
    val member = AddIdent(ns.user.role / Role.Member.toString)

    val schema = Seq(
      email, firstName, lastName, fullName, avatarURL, role,
      loginInfo,
      administrator, member
    )

  }

  implicit val kwToRole: datomisca.Keyword => Role = (kw: datomisca.Keyword) => Role(kw.getName)
  implicit val roleToKw: Role => datomisca.Keyword = (role: Role) => Schema.ns.user.role / role.name

  implicit val reader: EntityReader[User] = (
    ID.read[Long] and
    Schema.email.read[String] and
    Schema.firstName.readOpt[String] and
    Schema.lastName.readOpt[String] and
    Schema.fullName.readOpt[String] and
    Schema.avatarURL.readOpt[String] and
    Schema.role.readOrElse[Role](Role.Member)
  )(User.apply _)

  implicit val writer: PartialAddEntityWriter[User] = (
    ID.write[Long] and
    Schema.email.write[String] and
    Schema.firstName.writeOpt[String] and
    Schema.lastName.writeOpt[String] and
    Schema.fullName.writeOpt[String] and
    Schema.avatarURL.writeOpt[String] and
    Schema.role.write[Role]
  )(unlift(User.unapply))

  def findByLoginInfo(loginInfo: LoginInfo)(implicit conn: Connection): Option[User] = {
    val query = Query(
      """
    [
      :find ?e
      :in $ ?providerId ?providerKey
      :where
        [?l :loginInfo/providerId ?providerId]
        [?l :loginInfo/providerKey ?providerKey]
        [?e :user/loginInfo ?l]
    ]
      """)

    headOption(Datomic.q(query, Datomic.database, loginInfo.providerID, loginInfo.providerKey))

  }

  def create(user: User, loginInfo: LoginInfo)(implicit conn: Connection, ec: ExecutionContext): Future[User] = {
    implicit val loginInfoWriter = LoginInfoImpl.writer

    val loginInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(loginInfo)
    val userFact = DatomicMapping.toEntity(DId(Partition.USER))(user)
    val userLoginInfo = SchemaFact.add(userFact.id)(User.Schema.loginInfo -> loginInfoFact)

    val allFacts = Seq(loginInfoFact, userFact, userLoginInfo)

    for {
      tx <- Datomic.transact(allFacts)
    } yield User.get(tx.resolve(userFact))

  }

  def update(id: Long, user: User)(implicit conn: Connection, ec: ExecutionContext): Future[User] = {
    implicit val primaryId = id
    val o = User.get(id)

    val userFacts: Seq[TxData] = Seq(
      DB.factOrNone(o.email, user.email, Schema.email -> user.email),
      DB.factOrNone(o.firstName, user.firstName, Schema.firstName -> user.firstName.getOrElse("")),
      DB.factOrNone(o.lastName, user.lastName, Schema.lastName -> user.lastName.getOrElse("")),
      DB.factOrNone(o.fullName, user.fullName, Schema.fullName -> user.fullName.getOrElse("")),
      DB.factOrNone(o.avatarURL, user.avatarURL, Schema.avatarURL -> user.avatarURL.getOrElse("")),
      DB.factOrNone(o.role, user.role, Schema.role -> user.role)
    ).flatten

    for {
      tx <- Datomic.transact(userFacts)
    } yield user

  }

}
