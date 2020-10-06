package persistence.datomic.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import javax.inject.Inject
import persistence.datomic.DatomicAuthService

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import Queries._

/**
 * The DAO to persist the OAuth1 information.
 */
final class OpenIDInfoDAO @Inject() (implicit override val classTag: ClassTag[OpenIDInfo], myDatomisca: DatomicAuthService, ec: ExecutionContext)
  extends DelegableAuthInfoDAO[OpenIDInfo] {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Finds the auth info which is linked to the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The found auth info or None if no auth info could be found for the given login info.
   */
  override def find(loginInfo: LoginInfo): Future[Option[OpenIDInfo]] =
    Future.successful(OpenIDInfoImpl.findWithId(loginInfo).map(_._2))

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo  The auth info to add.
   * @return The added auth info.
   */
  override def add(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] =
    OpenIDInfoImpl.add(loginInfo, authInfo)

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo  The auth info to update.
   * @return The updated auth info.
   */
  override def update(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] =
    OpenIDInfoImpl.update(loginInfo, authInfo)

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
  override def save(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(LoginInfoImpl.remove(loginInfo)(conn, ec))

}

object OpenIDInfoImpl extends DB[OpenIDInfo] {

  object Schema {

    object ns {
      val openIdInfo = new Namespace("oauth1Info")
    }

    val id = Attribute(ns.openIdInfo / "id", SchemaType.string, Cardinality.one).withDoc("The openID")
    val attributes = Attribute(ns.openIdInfo / "attributes", SchemaType.string, Cardinality.one).withDoc("The attributes passed by the provider")

    val schema = Seq(
      id, attributes)

  }

  def mapToString(mapOpt: Map[String, String]): String = ""

  def stringToMap(string: String): Map[String, String] = Map[String, String]()

  implicit val reader: EntityReader[OpenIDInfo] = (
    Schema.id.read[String] and
    Schema.attributes.read[String].map(stringToMap))(OpenIDInfo.apply _)

  implicit val writer: PartialAddEntityWriter[OpenIDInfo] = (
    Schema.id.write[String] and
    Schema.attributes.write[String].contramap(mapToString))(unlift(OpenIDInfo.unapply))

  def findWithId(loginInfo: LoginInfo)(implicit conn: Connection): Option[(Long, OpenIDInfo)] = {
    val query = query"""
      [
        :find ?e
        :in $$ ?providerId ?providerKey
        :where
          [?l :loginInfo/providerId ?providerId]
          [?l :loginInfo/providerKey ?providerKey]
          [?l :loginInfo/openIdInfo ?e]
      ]
      """

    DB.headOptionWithId(Datomic.q(query, Datomic.database(), loginInfo.providerID, loginInfo.providerKey), Datomic.database())

  }

  def add(loginInfo: LoginInfo, oAuth1Info: OpenIDInfo)(implicit conn: Connection, ec: ExecutionContext): Future[OpenIDInfo] = {
    implicit val loginInfoWriter = persistence.datomic.daos.LoginInfoImpl.writer
    val oAuth1InfoFact = DatomicMapping.toEntity(DId(Partition.USER))(oAuth1Info)

    val loginInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(loginInfo)

    val loginInfoPasswordFact = SchemaFact.add(loginInfoFact.id)(LoginInfoImpl.Schema.oAuth1Info -> oAuth1InfoFact)

    for {
      tx <- Datomic.transact(loginInfoFact, oAuth1InfoFact, loginInfoPasswordFact)
    } yield OpenIDInfoImpl.get(tx.resolve(oAuth1InfoFact.id))

  }

  def update(loginInfo: LoginInfo, oAuth1Info: OpenIDInfo)(implicit conn: Connection, ec: ExecutionContext): Future[OpenIDInfo] = {

    val foundInfo: (Long, OpenIDInfo) = OpenIDInfoImpl.findWithId(loginInfo).get
    implicit val primaryId = foundInfo._1
    val o = foundInfo._2

    val passwordInfoFacts: Seq[TxData] = Seq(
      DB.factOrNone(o.id, oAuth1Info.id, Schema.id -> oAuth1Info.id),
      DB.factOrNone(o.attributes, oAuth1Info.attributes, Schema.attributes -> mapToString(oAuth1Info.attributes))).flatten

    for {
      tx <- Datomic.transact(passwordInfoFacts)
    } yield oAuth1Info

  }
}
