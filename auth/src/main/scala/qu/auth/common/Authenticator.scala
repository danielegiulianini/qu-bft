package qu.auth.common

import qu.auth.{Credentials, Token, User}

trait Authenticator {
  def register(user: User): Unit
  def authorize(credentials: Credentials): Token
}
