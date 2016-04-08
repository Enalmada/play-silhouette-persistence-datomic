package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }

/**
 * The user object.
 *
 * @param userID The unique ID of the user.
 * @param loginInfo The linked login info.
 * @param firstName Maybe the first name of the authenticated user.
 * @param lastName Maybe the last name of the authenticated user.
 * @param fullName Maybe the full name of the authenticated user.
 * @param email Maybe the email of the authenticated provider.
 * @param avatarURL Maybe the avatar URL of the authenticated provider.
 */
case class User(
  userID: UUID,
  loginInfo: LoginInfo,
  firstName: Option[String],
  lastName: Option[String],
  fullName: Option[String],
  email: Option[String],
  avatarURL: Option[String],
  roles: Set[Role] = Set(Role.Member)) extends Identity {

  final def hasRole(role: Role): Boolean = roles.contains(role) || roles.contains(Role.Tech)
  final lazy val isTech: Boolean = hasRole(Role.Tech)
  final lazy val isAdministrator: Boolean = hasRole(Role.Administrator)
  final lazy val isMember: Boolean = hasRole(Role.Member)

}

sealed abstract class Role(val name: String) {
  override def toString: String = name
}

object Role {
  case object Tech extends Role("tech")
  case object Administrator extends Role("administrator")
  case object Member extends Role("member")

  val all: Set[Role] = Set(Tech, Administrator, Member)

  def apply(name: String): Role = {
    all.find(s => s.name == name) match {
      case Some(role) => role
      case None => throw new IllegalArgumentException(s"Invalid Role: $name")
    }
  }

}