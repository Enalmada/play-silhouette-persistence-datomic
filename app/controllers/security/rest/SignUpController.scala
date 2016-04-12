package controllers.security.rest

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import models.{ User, UserService }
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import utils.auth.JwtEnv
import utils.persistence.DatomicService

import scala.concurrent.Future

/**
 * The sign up controller.
 *
 * @param messagesApi        The Play messages API.
 * @param silhouette         The Silhouette environment.
 * @param userService        The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService      The avatar service implementation.
 * @param passwordHasher     The password hasher implementation.
 */
class SignUpController @Inject() (
  val messagesApi: MessagesApi,
  silhouette: Silhouette[JwtEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  myDatomisca: DatomicService)
  extends Controller with I18nSupport {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Registers a new user.
   *
   * @return The result to display.
   */
  def signUp = Action.async(parse.json) { implicit request =>
    request.body.validate[SignUpForm.SignUpData].map { data =>
      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
      userService.retrieve(loginInfo).flatMap {
        case Some(user) =>
          Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
        case None =>
          val authInfo = passwordHasher.hash(data.password)
          val user = User(
            firstName = Some(data.firstName),
            lastName = Some(data.lastName),
            fullName = Some(data.firstName + " " + data.lastName),
            email = data.email,
            avatarURL = None
          )
          for {
            avatar <- avatarService.retrieveURL(data.email)
            user <- User.create(user.copy(avatarURL = avatar), loginInfo)
            authInfo <- authInfoRepository.add(loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(loginInfo)
            token <- silhouette.env.authenticatorService.init(authenticator)
          } yield {
            silhouette.env.eventBus.publish(SignUpEvent(user, request))
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            Ok(Json.obj("token" -> token))
          }
      }
    }.recoverTotal {
      case error =>
        Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.data"))))
    }
  }
}
