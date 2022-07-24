package qu.auth.server

import io.grpc.Status
import qu.auth._
import qu.auth.common.FutureUtilities.mapThrowableByStatus
import qu.auth.common.{AsyncAuthenticator, BadContentException, ConflictException, LocalAuthenticator, WrongCredentialsException}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service providing JWT-token-based authorization and authentication facilities for users.
 */
class AuthService(implicit ec:ExecutionContext) extends AuthGrpc.Auth with AsyncAuthenticator {

  private val localAuthenticator: LocalAuthenticator = new LocalAuthenticator

  //mapping custom exception to transport layer code for:
  //1. separation of concerns (not cluttering app logic code with transport layer error codes):
  // so separating a reusable localAuthenticator
  //2. StatusRuntimeException swallows the error message and assigns a default status code
  // UNKNOWN at client side.Failure(io.grpc.StatusRuntimeException: UNKNOWN)
  override def registerAsync(request: User): Future[RegisterResponse] = {
    //could delegate to external pool too
    mapThrowableByStatus(Future(
      localAuthenticator.register(request)
    ).map(_ => {
      RegisterResponse()
    }), {
      case _: BadContentException =>
        Status.INVALID_ARGUMENT  //already wrapping message inside mapThrowableByStatus
      case _: ConflictException =>
        Status.ALREADY_EXISTS
    })
  }

  override def authorizeAsync(request: Credentials): Future[Token] = {
    mapThrowableByStatus(Future(
      localAuthenticator.authorize(request)
    ), {
      case _: BadContentException => Status.INVALID_ARGUMENT
      case _: WrongCredentialsException => Status.NOT_FOUND
    })
  }
}