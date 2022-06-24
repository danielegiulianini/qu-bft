package qu.auth.common

import qu.auth.{Credentials, Token, User}

/**
 * Provides asynchronous, token-based, authentication APIs.
 */
trait Authenticator {
  def register(user: User): Unit
  def authorize(credentials: Credentials): Token
}
