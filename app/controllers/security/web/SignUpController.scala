package controllers.security.web

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.impl.providers._
import forms.SignUpForm
import javax.inject.Inject
import models.{ Role, User, UserService }
import play.api.i18n.{ I18nSupport, Messages }
import play.api.mvc.{ AbstractController, ControllerComponents }
import utils.auth.DefaultEnv
import utils.persistence.DatomicService

import scala.concurrent.{ ExecutionContext, Future }

/**
 * The `Sign Up` controller.
 *
 * @param messagesApi        The Play messages API.
 * @param silhouette         The Silhouette stack.
 * @param userService        The user service implementation.
 * @param authInfoRepository The auth info repository implementation.
 * @param avatarService      The avatar service implementation.
 * @param passwordHasher     The password hasher implementation.
 * @param webJarAssets       The webjar assets implementation.
 */
class SignUpController @Inject() (implicit
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  ec: ExecutionContext,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  myDatomisca: DatomicService)
  extends AbstractController(components) with I18nSupport {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Views the `Sign Up` page.
   *
   * @return The result to display.
   */
  def view = silhouette.UnsecuredAction.async(parse.default) { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form)))
  }

  /**
   * Handles the submitted form.
   *
   * @return The result to display.
   */
  def submit = silhouette.UnsecuredAction.async(parse.default) { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => {
        val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) =>
            Future.successful(Redirect(routes.SignUpController.view()).flashing("error" -> Messages("user.exists")))
          case None =>
            val authInfo = passwordHasher.hash(data.password)
            val user = User().copy(
              firstName = Some(data.firstName),
              lastName = Some(data.lastName),
              fullName = Some(data.firstName + " " + data.lastName),
              email = data.email,
              avatarURL = None,
              role = if (data.email.contains("enalmada@gmail.com")) {
                Role.Administrator
              } else {
                Role.Member
              }
            )
            for {
              avatar <- avatarService.retrieveURL(data.email)
              user <- User.create(user.copy(avatarURL = avatar), loginInfo)
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authenticator <- silhouette.env.authenticatorService.create(loginInfo)
              value <- silhouette.env.authenticatorService.init(authenticator)
              result <- silhouette.env.authenticatorService.embed(value, Redirect(controllers.routes.ApplicationController.index()))
            } yield {
              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              result
            }
        }
      }
    )
  }
}
