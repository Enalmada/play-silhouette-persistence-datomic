package controllers.security.web

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.Clock
import com.mohiva.play.silhouette.impl.providers._
import com.typesafe.config.Config
import controllers.WebJarAssets
import models.UserService
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{ Action, Controller }
import utils.auth.DefaultEnv
import ConfigPimping._
import scala.concurrent.Future
import scala.concurrent.duration.{ Duration, FiniteDuration }

/**
 * The social auth controller.
 *
 * @param messagesApi            The Play messages API.
 * @param silhouette             The Silhouette stack.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets           The webjar assets implementation.
 */
class SocialAuthController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  clock: Clock,
  implicit val webJarAssets: WebJarAssets)
  extends Controller with I18nSupport with Logger {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { implicit request =>
    val c = configuration.underlying

    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) => for {
            profile <- p.retrieveProfile(authInfo)
            user <- userService.save(profile)
            authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
            authenticator <- {
              silhouette.env.authenticatorService.create(profile.loginInfo).map { authenticator =>
                authenticator.copy(
                  expirationDateTime = clock.now.plus(c.getFiniteDuration("silhouette.authenticator.rememberMe.authenticatorExpiry").toMillis),
                  idleTimeout = c.getOptionalFiniteDuration("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                  cookieMaxAge = c.getOptionalFiniteDuration("silhouette.authenticator.rememberMe.cookieMaxAge")
                )
              }

            }

            value <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(value, Redirect(controllers.routes.ApplicationController.index()))
          } yield {
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            result
          }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.SignInController.view()).flashing("error" -> Messages("could.not.authenticate"))
    }
  }
}

object ConfigPimping {
  implicit class RichConfig(val underlying: Config) extends AnyVal {

    def getFiniteDuration(path: String): FiniteDuration = {
      Some(Duration(underlying.getString(path))).collect { case d: FiniteDuration => d }.get
    }

    def getOptionalFiniteDuration(path: String): Option[FiniteDuration] = if (underlying.hasPath(path)) {
      Some(Duration(underlying.getString(path))).collect { case d: FiniteDuration => d }
    } else {
      None
    }
  }
}