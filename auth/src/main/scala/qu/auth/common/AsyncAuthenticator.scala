package qu.auth.common

import qu.auth.{Credentials, RegisterResponse, Token, User}

import scala.concurrent.Future

trait AsyncAuthenticator {
  def registerAsync(request: User): Future[RegisterResponse]
  def authorizeAsync(request: Credentials): Future[Token]
}
