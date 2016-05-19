package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import models.{ Role, WithRole }
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.Controller
import utils.auth.DefaultEnv
import utils.persistence.DatomicService

import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi            The Play messages API.
 * @param silhouette             The Silhouette stack.
 * @param socialProviderRegistry The social provider registry.
 * @param webJarAssets           The webjar assets implementation.
 */
class ApplicationController @Inject() (implicit
  val messagesApi: MessagesApi,
  silhouette: Silhouette[DefaultEnv],
  socialProviderRegistry: SocialProviderRegistry,
  webJarAssets: WebJarAssets,
  datomicService: DatomicService) extends Controller with I18nSupport {

  implicit val conn = datomicService.conn
  protected[this] val e = conn

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = silhouette.SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.home(request.identity)))
  }

  def admin = silhouette.SecuredAction(WithRole(Role.Administrator)).async { implicit request =>
    Future.successful(Ok(views.html.admin(request.identity)))
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}
