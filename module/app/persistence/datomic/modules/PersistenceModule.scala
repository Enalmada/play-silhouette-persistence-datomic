package persistence.datomic.modules

import javax.inject

import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.{ OAuth1Info, OAuth2Info, OpenIDInfo }
import com.mohiva.play.silhouette.persistence.daos._
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.codingwell.scalaguice.ScalaModule
import persistence.datomic.daos._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Provides Guice bindings for the persistence module.
 */
class PersistenceModule extends AbstractModule with ScalaModule {

  /**
   * Configures the module.
   */
  def configure(): Unit = {
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDAO].in[inject.Singleton]
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDAO].in[inject.Singleton]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO].in[inject.Singleton]
    bind[DelegableAuthInfoDAO[OpenIDInfo]].to[OpenIDInfoDAO].in[inject.Singleton]
  }

  /**
   * Provides the auth info repository.
   *
   * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
   * @param oauth1InfoDAO   The implementation of the delegable OAuth1 auth info DAO.
   * @param oauth2InfoDAO   The implementation of the delegable OAuth2 auth info DAO.
   * @param openIDInfoDAO   The implementation of the delegable OpenID auth info DAO.
   * @return The auth info repository instance.
   */
  @Provides
  def provideAuthInfoRepository(
    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
    oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
    oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]
  ): AuthInfoRepository = {

    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)
  }
}
