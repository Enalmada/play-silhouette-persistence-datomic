package controllers.security.rest

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers._
import forms.SignInForm
import javax.inject.Inject
import models.UserService
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{ AbstractController, ControllerComponents }
import utils.auth.JwtEnv

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

/**
 * The credentials auth controller.
 *
 * @param messagesApi            The Play messages API.
 * @param silhouette             The Silhouette environment.
 * @param userService            The user service implementation.
 * @param authInfoRepository     The auth info repository implementation.
 * @param credentialsProvider    The credentials provider.
 * @param socialProviderRegistry The social provider registry.
 * @param configuration          The Play configuration.
 * @param clock                  The clock instance.
 */
class CredentialsAuthController @Inject() (
  implicit
  val components: ControllerComponents,
  messagesApi: MessagesApi,
  silhouette: Silhouette[JwtEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  socialProviderRegistry: SocialProviderRegistry,
  configuration: Configuration,
  ec: ExecutionContext,
  clock: Clock)
  extends AbstractController(components) with I18nSupport with Logger {

  /**
   * Converts the JSON into a `SignInForm.Data` object.
   */
  implicit val dataReads = (
    (__ \ 'email).read[String] and
    (__ \ 'password).read[String] and
    (__ \ 'rememberMe).read[Boolean]
  )(SignInForm.SignInData.apply _)

  /**
   * Authenticates a user against the credentials provider.
   *
   * @return The result to display.
   */
  def authenticate = Action.async(parse.json) { implicit request =>
    request.body.validate[SignInForm.SignInData].map { data =>
      credentialsProvider.authenticate(Credentials(data.email, data.password)).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator if data.rememberMe =>
              val c = configuration.underlying
              authenticator.copy(
                expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            silhouette.env.authenticatorService.init(authenticator).map { token =>
              Ok(Json.obj("token" -> token))
            }
          }
          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }.recover {
        case e: ProviderException =>
          Unauthorized(Json.obj("message" -> Messages("invalid.credentials")))
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.credentials"))))
    }
  }
}
