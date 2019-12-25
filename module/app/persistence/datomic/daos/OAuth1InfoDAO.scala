package persistence.datomic.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import javax.inject.Inject
import persistence.datomic.DatomicAuthService

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls

/**
 * The DAO to persist the OAuth1 information.
 */
final class OAuth1InfoDAO @Inject()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext)
  extends DelegableAuthInfoDAO[OAuth1Info] {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Finds the auth info which is linked to the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The found auth info or None if no auth info could be found for the given login info.
   */
  override def find(loginInfo: LoginInfo): Future[Option[OAuth1Info]] =
    Future.successful(OAuth1InfoImpl.findWithId(loginInfo).map(_._2))

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo  The auth info to add.
   * @return The added auth info.
   */
  override def add(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] =
    OAuth1InfoImpl.add(loginInfo, authInfo)

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo  The auth info to update.
   * @return The updated auth info.
   */
  override def update(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] =
    OAuth1InfoImpl.update(loginInfo, authInfo)

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
  override def save(loginInfo: LoginInfo, authInfo: OAuth1Info): Future[OAuth1Info] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(LoginInfoImpl.remove(loginInfo)(conn, ec))

}

object OAuth1InfoImpl extends DB[OAuth1Info] {

  object Schema {

    object ns {
      val oAuth1Info = new Namespace("oAuth1Info")
    }

    val token = Attribute(ns.oAuth1Info / "token", SchemaType.string, Cardinality.one).withDoc("The consumer token")
    val secret = Attribute(ns.oAuth1Info / "secret", SchemaType.string, Cardinality.one).withDoc("The consumer secret")

    val schema = Seq(
      token, secret)

  }

  implicit val reader: EntityReader[OAuth1Info] = (
    Schema.token.read[String] and
      Schema.secret.read[String]) (OAuth1Info.apply _)

  implicit val writer: PartialAddEntityWriter[OAuth1Info] = (
    Schema.token.write[String] and
      Schema.secret.write[String]) (unlift(OAuth1Info.unapply))

  def findWithId(loginInfo: LoginInfo)(implicit conn: Connection): Option[(Long, OAuth1Info)] = {
    val query = Query(
      """
      [
        :find ?e
        :in $ ?providerId ?providerKey
        :where
          [?l :loginInfo/providerId ?providerId]
          [?l :loginInfo/providerKey ?providerKey]
          [?l :loginInfo/oAuth1Info ?e]
      ]
      """)

    DB.headOptionWithId(Datomic.q(query, Datomic.database, loginInfo.providerID, loginInfo.providerKey), Datomic.database())

  }

  def add(loginInfo: LoginInfo, oAuth1Info: OAuth1Info)(implicit conn: Connection, ec: ExecutionContext): Future[OAuth1Info] = {
    implicit val loginInfoWriter = persistence.datomic.daos.LoginInfoImpl.writer
    val oAuth1InfoFact = DatomicMapping.toEntity(DId(Partition.USER))(oAuth1Info)

    val loginInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(loginInfo)

    val loginInfoPasswordFact = SchemaFact.add(loginInfoFact.id)(LoginInfoImpl.Schema.oAuth1Info -> oAuth1InfoFact)

    for {
      tx <- Datomic.transact(loginInfoFact, oAuth1InfoFact, loginInfoPasswordFact)
    } yield OAuth1InfoImpl.get(tx.resolve(oAuth1InfoFact.id))

  }

  def update(loginInfo: LoginInfo, oAuth1Info: OAuth1Info)(implicit conn: Connection, ec: ExecutionContext): Future[OAuth1Info] = {

    val foundInfo: (Long, OAuth1Info) = OAuth1InfoImpl.findWithId(loginInfo).get
    implicit val primaryId = foundInfo._1
    val o = foundInfo._2

    val passwordInfoFacts: Seq[TxData] = Seq(
      DB.factOrNone(o.token, oAuth1Info.token, Schema.token -> oAuth1Info.token),
      DB.factOrNone(o.secret, oAuth1Info.secret, Schema.secret -> oAuth1Info.secret)).flatten

    for {
      tx <- Datomic.transact(passwordInfoFacts)
    } yield oAuth1Info

  }
}
