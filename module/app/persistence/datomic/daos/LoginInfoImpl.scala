package persistence.datomic.daos

import com.mohiva.play.silhouette.api.LoginInfo
import datomisca.DatomicMapping._
import datomisca._
import datomiscadao.DB

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.reflectiveCalls

object LoginInfoImpl extends DB[LoginInfo] {

  object Schema {

    object ns {
      val loginInfo = new Namespace("loginInfo")
    }

    val providerId = Attribute(ns.loginInfo / "providerId", SchemaType.string, Cardinality.one).withDoc("provider id")
    val providerKey = Attribute(ns.loginInfo / "providerKey", SchemaType.string, Cardinality.one).withDoc("provider key")

    val passwordInfo = Attribute(ns.loginInfo / "passwordInfo", SchemaType.ref, Cardinality.one).withIsComponent(true).withDoc("password info")
    val oAuth1Info = Attribute(ns.loginInfo / "oAuth1Info", SchemaType.ref, Cardinality.one).withIsComponent(true).withDoc("oauth1 info")
    val oAuth2Info = Attribute(ns.loginInfo / "oAuth2Info", SchemaType.ref, Cardinality.one).withIsComponent(true).withDoc("oauth2 info")
    val openIdInfo = Attribute(ns.loginInfo / "openIdInfo", SchemaType.ref, Cardinality.one).withIsComponent(true).withDoc("oauth2 info")

    val schema = Seq(
      providerId, providerKey,
      passwordInfo, oAuth1Info, oAuth2Info, openIdInfo)

  }

  implicit val reader: EntityReader[LoginInfo] = (
    Schema.providerId.read[String] and
    Schema.providerKey.read[String])(LoginInfo.apply _)

  implicit val writer: PartialAddEntityWriter[LoginInfo] = (
    Schema.providerId.write[String] and
    Schema.providerKey.write[String])(unlift(LoginInfo.unapply))

  def find(loginInfo: LoginInfo)(implicit conn: Connection): Option[Long] = {
    val query = Query(
      """
    [
      :find ?l
      :in $ ?providerId ?providerKey
      :where
        [?l :loginInfo/providerId ?providerId]
        [?l :loginInfo/providerKey ?providerKey]
    ]
      """)

    LoginInfoImpl.headOptionWithId(Datomic.q(query, Datomic.database, loginInfo.providerID, loginInfo.providerKey)).map(_._1)

  }

  def remove(loginInfo: LoginInfo)(implicit conn: Connection): Unit = find(loginInfo).map(remove)

  def remove(id: Long)(implicit conn: Connection): Unit = LoginInfoImpl.retractEntity(id)

}
