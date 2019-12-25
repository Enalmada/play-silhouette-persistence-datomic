package persistence.datomic.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import javax.inject.Inject
import persistence.datomic.DatomicAuthService

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

/**
 * The DAO to persist the password information.
 *
 * Note: Not thread safe, demo only.
 */
final class PasswordInfoDAO @Inject()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo] {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Finds the auth info which is linked to the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The found auth info or None if no auth info could be found for the given login info.
   */
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    Future.successful(PasswordInfoImpl.findWithId(loginInfo).map(_._2))

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo  The auth info to add.
   * @return The added auth info.
   */
  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    PasswordInfoImpl.add(loginInfo, authInfo)

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo  The auth info to update.
   * @return The updated auth info.
   */
  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] =
    PasswordInfoImpl.update(loginInfo, authInfo)

  /**
   * Saves the auth info for the given login info.
   *
   * This method either adds the auth info if it doesn't exists or it updates the auth info
   * if it already exists.
   *
   * @param loginInfo The login info for which the auth info should be saved.
   * @param authInfo  The auth info to save.
   * @return The saved auth info.
   */
  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(LoginInfoImpl.remove(loginInfo)(conn, ec))

}

object PasswordInfoImpl extends DB[PasswordInfo] {

  object Schema {

    object ns {
      val passwordInfo = new Namespace("passwordInfo")
    }

    val hasher = Attribute(ns.passwordInfo / "hasher", SchemaType.string, Cardinality.one).withDoc("hasher")
    val password = Attribute(ns.passwordInfo / "password", SchemaType.string, Cardinality.one).withDoc("password")
    val salt = Attribute(ns.passwordInfo / "salt", SchemaType.string, Cardinality.one).withDoc("salt")

    val schema = Seq(
      hasher, password, salt)

  }

  implicit val reader: EntityReader[PasswordInfo] = (
    Schema.hasher.read[String] and
      Schema.password.read[String] and
      Schema.salt.readOpt[String]) (PasswordInfo.apply _)

  implicit val writer: PartialAddEntityWriter[PasswordInfo] = (
    Schema.hasher.write[String] and
      Schema.password.write[String] and
      Schema.salt.writeOpt[String]) (unlift(PasswordInfo.unapply))

  def findWithId(loginInfo: LoginInfo)(implicit conn: Connection): Option[(Long, PasswordInfo)] = {
    val query = Query(
      """
    [
      :find ?e
      :in $ ?providerId ?providerKey
      :where
        [?l :loginInfo/providerId ?providerId]
        [?l :loginInfo/providerKey ?providerKey]
        [?l :loginInfo/passwordInfo ?e]
    ]
      """)

    DB.headOptionWithId(Datomic.q(query, Datomic.database, loginInfo.providerID, loginInfo.providerKey), Datomic.database())

  }

  def add(loginInfo: LoginInfo, passwordInfo: PasswordInfo)(implicit conn: Connection, ec: ExecutionContext): Future[PasswordInfo] = {
    implicit val loginInfoWriter = persistence.datomic.daos.LoginInfoImpl.writer
    val passwordInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(passwordInfo)

    val loginInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(loginInfo)

    val loginInfoPasswordFact = SchemaFact.add(loginInfoFact.id)(LoginInfoImpl.Schema.passwordInfo -> passwordInfoFact)

    for {
      tx <- Datomic.transact(loginInfoFact, passwordInfoFact, loginInfoPasswordFact)
    } yield PasswordInfoImpl.get(tx.resolve(passwordInfoFact.id))

  }

  def update(loginInfo: LoginInfo, passwordInfo: PasswordInfo)(implicit conn: Connection, ec: ExecutionContext): Future[PasswordInfo] = {

    val foundInfo: (Long, PasswordInfo) = PasswordInfoImpl.findWithId(loginInfo).get
    implicit val primaryId = foundInfo._1
    val o = foundInfo._2

    val passwordInfoFacts: Seq[TxData] = Seq(
      DB.factOrNone(o.hasher, passwordInfo.hasher, Schema.hasher -> passwordInfo.hasher),
      DB.factOrNone(o.password, passwordInfo.password, Schema.password -> passwordInfo.password),
      DB.factOrNone(o.salt, passwordInfo.salt, Schema.salt -> passwordInfo.salt.getOrElse(""))).flatten

    for {
      tx <- Datomic.transact(passwordInfoFacts)
    } yield passwordInfo

  }

}
