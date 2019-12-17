package models

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import javax.inject.Inject
import utils.persistence.DatomicService

import scala.concurrent.{ ExecutionContext, Future }

/**
 * Handles actions to users.
 */
class UserServiceImpl @Inject() (implicit myDatomisca: DatomicService, ec: ExecutionContext) extends UserService {

  implicit val conn = myDatomisca.conn
  protected[this] val e = conn

  /**
   * Retrieves a user that matches the specified login info.
   *
   * @param loginInfo The login info to retrieve a user.
   * @return The retrieved user or None if no user could be retrieved for the given login info.
   */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = Future.successful(User.findByLoginInfo(loginInfo))

  /**
   * Saves the social profile for a user.
   *
   * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
   *
   * @param profile The social profile to save.
   * @return The user for whom the profile was saved.
   */
  def save(profile: CommonSocialProfile): Future[User] = {

    User.findByLoginInfo(profile.loginInfo) match {
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
