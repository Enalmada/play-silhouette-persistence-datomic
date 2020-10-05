package persistence.datomic.modules

import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.providers.{ OAuth1Info, OAuth2Info, OpenIDInfo }
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.codingwell.scalaguice.ScalaModule
import persistence.datomic.DatomicAuthService
import persistence.datomic.daos.{ OAuth1InfoDAO, OAuth2InfoDAO, OpenIDInfoDAO, PasswordInfoDAO }

import scala.concurrent.ExecutionContext

/**
 * Provides Guice bindings for the persistence module.
 */
class PersistenceModule extends AbstractModule with ScalaModule {

  /*
  override def configure(): Unit = {
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDAO].in[inject.Singleton]
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDAO].in[inject.Singleton]
    bind[DelegableAuthInfoDAO[OpenIDInfo]].to[OpenIDInfoDAO].in[inject.Singleton]
  }

   */

  @Provides
  def providePasswordDAO()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext): DelegableAuthInfoDAO[PasswordInfo] = new PasswordInfoDAO()

  @Provides
  def provideOAuth1InfoDAO()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext): DelegableAuthInfoDAO[OAuth1Info] = new OAuth1InfoDAO()

  @Provides
  def provideOAuth2InfoDAO()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext): DelegableAuthInfoDAO[OAuth2Info] = new OAuth2InfoDAO()

  @Provides
  def provideOpenIDInfoDAO()(implicit myDatomisca: DatomicAuthService, ec: ExecutionContext): DelegableAuthInfoDAO[OpenIDInfo] = new OpenIDInfoDAO()

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
    openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo])(implicit ec: ExecutionContext): AuthInfoRepository = {

    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)
  }
}
