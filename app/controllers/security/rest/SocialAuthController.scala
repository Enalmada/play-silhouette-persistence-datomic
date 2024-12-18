package controllers.security.rest

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.impl.providers._
import javax.inject.Inject
import models.UserService
import play.api.Logging
import play.api.cache.SyncCacheApi
import play.api.i18n.{ I18nSupport, Messages }
import play.api.libs.json.Json
import play.api.mvc._
import utils.auth.JwtEnv

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

/**
 * The social auth controller.
 *
 * @param silhouette             The Silhouette environment.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info service implementation.
 * @param socialProviderRegistry The social provider registry.
 */
class SocialAuthController @Inject() (
  implicit
  ec: ExecutionContext,
  components: ControllerComponents,
  silhouette: Silhouette[JwtEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  socialProviderRegistry: SocialProviderRegistry,
  cache: SyncCacheApi) extends AbstractController(components) with I18nSupport with Logging {

  /**
   * Authenticates a user against a social provider.
   *
   * @param provider The ID of the provider to authenticate against.
   * @return The result to display.
   */
  def authenticate(provider: String) = Action.async { r =>
    cacheAuthTokenForOauth1(r) { implicit request =>
      (socialProviderRegistry.get[SocialProvider](provider) match {
        case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
          p.authenticate().flatMap {
            case Left(result) => Future.successful(result)
            case Right(authInfo) => for {
              profile <- p.retrieveProfile(authInfo)
              user <- userService.save(profile)
              authInfo <- authInfoRepository.save(profile.loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
              token <- silhouette.env.authenticatorService.init(authenticator)
            } yield {
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              Ok(Json.obj("token" -> token))
            }
          }
        case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
      }).recover {
        case e: ProviderException =>
          logger.error("Unexpected provider error", e)
          Unauthorized(Json.obj("message" -> Messages("could.not.authenticate")))
      }
    }
  }

  /**
   * Satellizer executes multiple requests to the same application endpoints for OAuth1.
   *
   * So this function caches the response from the OAuth provider and returns it on the second
   * request. Not nice, but it works as a temporary workaround until the bug is fixed.
   *
   * @param request The current request.
   * @param f       The action to execute.
   * @return A result.
   * @see https://github.com/sahat/satellizer/issues/287
   */
  private def cacheAuthTokenForOauth1(request: Request[AnyContent])(f: Request[AnyContent] => Future[Result]): Future[Result] = {
    request.getQueryString("oauth_token") -> request.getQueryString("oauth_verifier") match {
      case (Some(token), Some(verifier)) => cache.get[Result](token + "-" + verifier) match {
        case Some(result) => Future.successful(result)
        case None => f(request).map { result =>
          cache.set(token + "-" + verifier, result, 1 minute)
          result
        }
      }
      case _ => f(request)
    }
  }
}
