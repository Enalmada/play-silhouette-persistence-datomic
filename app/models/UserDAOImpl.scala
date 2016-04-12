package models

import com.mohiva.play.silhouette.api.LoginInfo
import datomisca.Connection

import scala.concurrent.Future

/**
 * Give access to the user object.
 */
class UserDAOImpl extends UserDAO {

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo)(implicit conn: Connection) = Future.successful(User.findByLoginInfo(loginInfo))

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: Long)(implicit conn: Connection) = {
    Future.successful(User.find(userID))
  }

}
