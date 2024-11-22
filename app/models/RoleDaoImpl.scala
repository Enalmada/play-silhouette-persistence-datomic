package models

import play.silhouette.api.Authorization
import play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.mvc.Request

import scala.concurrent.Future

case class WithRole(role: Role) extends Authorization[User, CookieAuthenticator] {

  def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request: Request[B]) = {
    Future.successful(user.hasRole(role))
  }
}
