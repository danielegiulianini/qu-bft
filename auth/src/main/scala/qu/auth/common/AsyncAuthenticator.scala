package qu.auth.common

import qu.auth.{Credentials, RegisterResponse, Token, User}

import scala.concurrent.Future

/**
 * Provides asynchronous, token-based, authentication APIs by returning [[scala.concurrent.Future]]s.
 */
trait AsyncAuthenticator {
  def registerAsync(request: User): Future[RegisterResponse]
  def authorizeAsync(request: Credentials): Future[Token]
}
