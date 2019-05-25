package persistence.datomic.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB
import persistence.datomic.DatomicAuthService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls

/**
 * The DAO to persist the OAuth2 information.
 */
final class OAuth2InfoDAO @Inject() (myDatomisca: DatomicAuthService)
  extends DelegableAuthInfoDAO[OAuth2Info] {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Finds the auth info which is linked to the specified login info.
   *
   * @param loginInfo The linked login info.
   * @return The found auth info or None if no auth info could be found for the given login info.
   */
  override def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] =
    Future.successful(OAuth2InfoImpl.findWithId(loginInfo).map(_._2))

  /**
   * Adds new auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be added.
   * @param authInfo  The auth info to add.
   * @return The added auth info.
   */
  override def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] =
    OAuth2InfoImpl.add(loginInfo, authInfo)

  /**
   * Updates the auth info for the given login info.
   *
   * @param loginInfo The login info for which the auth info should be updated.
   * @param authInfo  The auth info to update.
   * @return The updated auth info.
   */
  override def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] =
    OAuth2InfoImpl.update(loginInfo, authInfo)

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
  override def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(LoginInfoImpl.remove(loginInfo)(conn))

}

object OAuth2InfoImpl extends DB[OAuth2Info] {

  object Schema {

    object ns {
      val oAuth2Info = new Namespace("oAuth2Info")
    }

    val accessToken = Attribute(ns.oAuth2Info / "accesstoken", SchemaType.string, Cardinality.one).withDoc("The access token")
    val tokenType = Attribute(ns.oAuth2Info / "tokentype", SchemaType.string, Cardinality.one).withDoc("The token type")
    val expiresIn = Attribute(ns.oAuth2Info / "expiresIn", SchemaType.long, Cardinality.one).withDoc("The number of seconds before the token expires")
    val refreshToken = Attribute(ns.oAuth2Info / "refreshToken", SchemaType.string, Cardinality.one).withDoc("The refresh token.")
    val params = Attribute(ns.oAuth2Info / "params", SchemaType.string, Cardinality.one).withDoc("Additional params transported in conjunction with the token")

    val schema = Seq(
      accessToken, tokenType, expiresIn, refreshToken, params)

  }

  def mapToString(mapOpt: Option[Map[String, String]]): Option[String] = None

  def stringToMap(string: Option[String]): Option[Map[String, String]] = None

  implicit val reader: EntityReader[OAuth2Info] = (
    Schema.accessToken.read[String] and
    Schema.tokenType.readOpt[String] and
    Schema.expiresIn.readOpt[Int] and
    Schema.refreshToken.readOpt[String] and
    Schema.params.readOpt[String].map(stringToMap))(OAuth2Info.apply _)

  implicit val writer: PartialAddEntityWriter[OAuth2Info] = (
    Schema.accessToken.write[String] and
    Schema.tokenType.writeOpt[String] and
    Schema.expiresIn.writeOpt[Int] and
    Schema.refreshToken.writeOpt[String] and
    Schema.params.writeOpt[String].contramap(mapToString))(unlift(OAuth2Info.unapply))

  def findWithId(loginInfo: LoginInfo)(implicit conn: Connection): Option[(Long, OAuth2Info)] = {
    val query = Query(
      """
    [
      :find ?e
      :in $ ?providerId ?providerKey
      :where
        [?l :loginInfo/providerId ?providerId]
        [?l :loginInfo/providerKey ?providerKey]
        [?l :loginInfo/oAuth2Info ?e]
    ]
      """)

    DB.headOptionWithId(Datomic.q(query, Datomic.database, loginInfo.providerID, loginInfo.providerKey), Datomic.database())

  }

  def add(loginInfo: LoginInfo, oAuth2Info: OAuth2Info)(implicit conn: Connection): Future[OAuth2Info] = {
    implicit val loginInfoWriter = persistence.datomic.daos.LoginInfoImpl.writer
    val oAuth2InfoFact = DatomicMapping.toEntity(DId(Partition.USER))(oAuth2Info)

    val loginInfoFact = DatomicMapping.toEntity(DId(Partition.USER))(loginInfo)

    val loginInfoPasswordFact = SchemaFact.add(loginInfoFact.id)(LoginInfoImpl.Schema.oAuth2Info -> oAuth2InfoFact)

    for {
      tx <- Datomic.transact(loginInfoFact, oAuth2InfoFact, loginInfoPasswordFact)
    } yield OAuth2InfoImpl.get(tx.resolve(oAuth2InfoFact.id))

  }

  def update(loginInfo: LoginInfo, oAuth2Info: OAuth2Info)(implicit conn: Connection): Future[OAuth2Info] = {

    val foundInfo: (Long, OAuth2Info) = OAuth2InfoImpl.findWithId(loginInfo).get
    implicit val primaryId = foundInfo._1
    val o = foundInfo._2

    val passwordInfoFacts: Seq[TxData] = Seq(
      DB.factOrNone(o.accessToken, oAuth2Info.accessToken, Schema.accessToken -> oAuth2Info.accessToken),
      DB.factOrNone(o.tokenType, oAuth2Info.tokenType, Schema.tokenType -> oAuth2Info.tokenType.getOrElse("")),
      DB.factOrNone(o.expiresIn, oAuth2Info.expiresIn, Schema.expiresIn -> oAuth2Info.expiresIn.getOrElse(0)),
      DB.factOrNone(o.refreshToken, oAuth2Info.refreshToken, Schema.refreshToken -> oAuth2Info.refreshToken.getOrElse("")),
      DB.factOrNone(o.params, oAuth2Info.params, Schema.params -> mapToString(oAuth2Info.params).getOrElse(""))).flatten

    for {
      tx <- Datomic.transact(passwordInfoFacts)
    } yield oAuth2Info

  }

}
