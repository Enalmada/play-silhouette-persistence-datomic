package controllers.security.web

import java.util.UUID

import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordHasher, PasswordInfo }
import com.mohiva.play.silhouette.api.{ Silhouette, _ }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import controllers.security.web.PasswordChangeController.ChangeInfo
import javax.inject.Inject
import models.UserService
import persistence.datomic.TokenUser
import persistence.datomic.services.TokenService
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.i18n._
import play.api.mvc._
import utils.auth.DefaultEnv
import utils.{ MailService, Mailer }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.util.{ Failure, Success }

/**
 * A controller to provide password change functionality
 */
class PasswordChangeController @Inject() (implicit
  ec: ExecutionContext,
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  userService: UserService,
  authInfoRepository: AuthInfoRepository,
  credentialsProvider: CredentialsProvider,
  avatarService: AvatarService,
  passwordHasher: PasswordHasher,
  tokenService: TokenService[TokenUser],
  mailService: MailService)
  extends AbstractController(components) with I18nSupport with Logging {

  val providerId = CredentialsProvider.ID
  val Email = "email"
  val passwordValidation = nonEmptyText(minLength = 6)

  /*
   * PASSWORD RESET  - When user has forgotten their password and can't login
   */

  val pwResetForm = Form[String](
    Email -> email.verifying(nonEmpty)
  )

  def passwordsForm()(implicit messagesProvider: MessagesProvider) = Form(tuple(
    "password1" -> passwordValidation,
    "password2" -> nonEmptyText,
    "token" -> uuid
  ) verifying (messagesProvider.messages("passwords.not.equal"), passwords => passwords._2 == passwords._1))

  private def notFoundDefault(implicit request: RequestHeader) =
    Future.successful(NotFound(views.html.auth.invalidToken()))

  // Action and parse now use the injected components
  def foo = Action(parse.default) { implicit request =>
    Ok(views.html.auth.startResetPassword(pwResetForm))
  }

  def startResetPassword = Action(parse.default) { implicit request =>
    Ok(views.html.auth.startResetPassword(pwResetForm))
  }

  def handleStartResetPassword = Action.async(parse.default) { implicit request =>
    pwResetForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.auth.startResetPassword(errors))),
      email => {
        authInfoRepository.find(LoginInfo(CredentialsProvider.ID, email))(ClassTag(classOf[PasswordInfo])).map {
          case Some(user) => {
            val newToken = new TokenUser(email = email)
            tokenService.create(newToken).onComplete {
              case Success(tokenUserOpt) => {
                tokenUserOpt.foreach { tokenUser =>
                  Mailer.forgotPassword(email, link = routes.PasswordChangeController.specifyResetPassword(tokenUser.id).absoluteURL())
                }
              }
              case Failure(t) => logger.error("handleStartResetPassword: " + t.getMessage)
            }

          }
          case None => {
            logger.info(s"handleStartResetPassword: no user found: ${email}")
            // Don't send out to unknown
            //Mailer.forgotPasswordUnknowAddress(email)(mailService)

          }
        }
        Future.successful(Ok(views.html.auth.sentResetPassword(email)))
      }
    )
  }

  /**
   * Confirms the user's link based on the token and shows them a form to reset the password
   */
  def specifyResetPassword(tokenId: UUID) = Action.async(parse.default) { implicit request =>
    tokenService.retrieve(tokenId).flatMap {
      case Some(token) if (!token.isSignUp && !token.isExpired) => {
        Future.successful(Ok(views.html.auth.specifyResetPassword(tokenId.toString, passwordsForm)))
      }
      case Some(token) => {
        tokenService.consume(tokenId)
        notFoundDefault
      }
      case None => {
        notFoundDefault
      }
    }
  }

  /**
   * Saves the new password and authenticates the user
   */
  def handleResetPassword = Action.async(parse.default) { implicit request =>
    passwordsForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.specifyResetPassword(formWithErrors.data("token"), formWithErrors))),
      passwords => {
        val tokenId = passwords._3
        tokenService.retrieve(tokenId).flatMap {
          case Some(token) if (!token.isSignUp && !token.isExpired) => {
            val loginInfo = LoginInfo(CredentialsProvider.ID, token.email)
            userService.retrieve(loginInfo).flatMap {
              case Some(user) => {
                val authInfo = passwordHasher.hash(passwords._1)
                authInfoRepository.save(loginInfo, authInfo)
                silhouette.env.authenticatorService.create(loginInfo).flatMap { authenticator =>
                  silhouette.env.eventBus.publish(LoginEvent(user, request))
                  tokenService.consume(tokenId)
                  silhouette.env.authenticatorService.init(authenticator)
                  Future.successful(Ok(views.html.auth.confirmResetPassword(user)))
                }
              }
              case None => Future.failed(new RuntimeException("Couldn't find user"))
            }
          }
          case Some(token) => {
            tokenService.consume(tokenId)
            notFoundDefault
          }
          case None => {
            notFoundDefault
          }
        }
      }
    )
  }

  /*
   * CHANGE PASSWORD - Can only be done whilst user is logged in
   */

  def changePasswordForm()(implicit messagesProvider: MessagesProvider) = Form[ChangeInfo](
    mapping(
      "currentPassword" -> nonEmptyText,
      "newPassword" -> tuple(
        "password1" -> passwordValidation,
        "password2" -> nonEmptyText
      ).verifying(messagesProvider.messages("passwords.not.equal"), newPassword => newPassword._2 == newPassword._1)
    )((currentPassword, newPassword) => ChangeInfo(currentPassword, newPassword._1)) //apply
    (data => Some((data.currentPassword, (data.newPassword, data.newPassword)))) //unapply
  )

  def startChangePassword = silhouette.SecuredAction.async(parse.default) { implicit request =>
    Future.successful(Ok(views.html.auth.changePassword(request.identity, changePasswordForm)))
  }

  /**
   * Saves the new password and authenticates the user
   */
  def handleChangePassword = silhouette.SecuredAction.async(parse.default) { implicit request =>

    changePasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.auth.changePassword(request.identity, formWithErrors))),
      changeInfo => {
        val user = request.identity

        credentialsProvider.authenticate(Credentials(user.email, changeInfo.currentPassword)).flatMap { loginInfo =>
          authInfoRepository.save(loginInfo, passwordHasher.hash(changeInfo.newPassword))
          silhouette.env.authenticatorService.create(loginInfo).flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            silhouette.env.authenticatorService.init(authenticator)
            Future.successful(Ok(views.html.auth.confirmResetPassword(user)))
          }
        }.recover {
          case e: ProviderException =>
            BadRequest(views.html.auth.changePassword(request.identity, changePasswordForm.withError("currentPassword", "Does not match current password!")))
        }
      }
    )
  }
}

object PasswordChangeController {

  case class ChangeInfo(currentPassword: String, newPassword: String)

}
