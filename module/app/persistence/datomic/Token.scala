package persistence.datomic

import java.util.UUID

/**
 * A token used for reset password and sign up operations.
 */
trait Token {

  /**
   * Gets the token ID.
   *
   * @return The token ID.
   */
  def id: UUID

}
