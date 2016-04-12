package models

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import persistence.datomic.DatomicAuthService
import play.api.libs.concurrent.Execution.Implicits._
import utils.persistence.DatomicService

import scala.concurrent.Future

/**
 * Handles actions to users.
 *
 * @param userDAO The user DAO implementation.
 */
class UserServiceImpl @Inject() (userDAO: UserDAO, myDatomisca: DatomicService) extends UserService {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[User] = {

    userDAO.find(profile.loginInfo).flatMap {
      case Some(user) => // Update user with profile
        User.update(user.id, user.copy(
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          email = profile.email.get,
          avatarURL = profile.avatarURL
        ))
      case None => // Insert a new user
        User.create(User(
          firstName = profile.firstName,
          lastName = profile.lastName,
          fullName = profile.fullName,
          email = profile.email.get,
          avatarURL = profile.avatarURL
        ), profile.loginInfo)
    }

  }
}