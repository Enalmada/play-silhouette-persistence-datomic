package controllers

import datomisca.Connection
import play.silhouette.api.{LogoutEvent, Silhouette}

import javax.inject.Inject
import models.{Role, WithRole}
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import utils.auth.DefaultEnv
import utils.persistence.DatomicService

import scala.concurrent.Future

/**
 * The basic application controller.
 *
 * @param messagesApi  The Play messages API.
 * @param silhouette   The Silhouette stack.
 * @param webJarAssets The webjar assets implementation.
 */
class ApplicationController @Inject() (implicit
  components: ControllerComponents,
  silhouette: Silhouette[DefaultEnv],
  datomicService: DatomicService) extends AbstractController(components) with I18nSupport {

  implicit val conn: Connection = datomicService.conn
  protected[this] val e: Connection = conn

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = silhouette.SecuredAction.async(parse.default) { implicit request =>
    Future.successful(Ok(views.html.home(request.identity)))
  }

  def admin = silhouette.SecuredAction(WithRole(Role.Administrator)).async(parse.default) { implicit request =>
    Future.successful(Ok(views.html.admin(request.identity)))
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = silhouette.SecuredAction.async(parse.default) { implicit request =>
    val result = Redirect(routes.ApplicationController.index)
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}
