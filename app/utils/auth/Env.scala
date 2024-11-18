package utils.auth

import play.silhouette.api.Env
import play.silhouette.impl.authenticators.{ CookieAuthenticator, JWTAuthenticator }
import models.User

/**
 * The default env.
 */
trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}

trait JwtEnv extends Env {
  type I = User
  type A = JWTAuthenticator
}
