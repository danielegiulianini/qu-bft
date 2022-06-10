package qu.auth

import FutureUtilities.mapThrowableByStatus
import io.grpc.Status
import qu.auth._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MyAuthService extends AuthGrpc.Auth {

  private val localAuthenticator: LocalAuthenticator = new LocalAuthenticator

  //mapping custom exception to transport layer code for:
  //1. separation of concerns (not cluttering app logic code with transport layer error codes): so separating a reusable localAuthenticator
  //2. StatusRuntimeException swallows the error message and assigns a default status code UNKNOWN at client side.Failure(io.grpc.StatusRuntimeException: UNKNOWN)
  override def register(request: User): Future[RegisterResponse] = {
    //se c'è problema nel local authenticator devo settare l'onError dell'observer
    //todo or delegate to external pool?
    mapThrowableByStatus(Future.successful(
      localAuthenticator.register(request)
    ).map(_ => RegisterResponse()), {
      case _: BadContentException => Status.INVALID_ARGUMENT
      case _: ConflictException => Status.ALREADY_EXISTS
    })
  }

  override def authorize(request: Credentials): Future[Token] = {
    mapThrowableByStatus(Future.successful(
      localAuthenticator.authorize(request)
    ), {
      case _: BadContentException => Status.INVALID_ARGUMENT
      case _: WrongCredentialsException => Status.NOT_FOUND
    })
  }




}
/*
object AttemptsForHighLevelWrapper {
  AuthGrpc.Auth.serviceCompanion.
}
*/