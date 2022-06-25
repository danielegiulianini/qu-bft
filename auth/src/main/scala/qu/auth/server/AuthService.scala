package qu.auth.server

import io.grpc.Status
import qu.auth._
import qu.auth.common.FutureUtilities.mapThrowableByStatus
import qu.auth.common.{AsyncAuthenticator, BadContentException, ConflictException, LocalAuthenticator, WrongCredentialsException}

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}


class AuthService(implicit ec:ExecutionContext) extends AuthGrpc.Auth with AsyncAuthenticator {

  private val localAuthenticator: LocalAuthenticator = new LocalAuthenticator
  private val logger = Logger.getLogger(classOf[AuthService].getName)

  private def logA(level: Level = Level.INFO, msg: String, param1: Int = 2)(implicit ec: ExecutionContext) = Future {
    logger.log(Level.WARNING, msg)
  }

  private def log(level: Level = Level.INFO, msg: String, param1: Int = 2) =
    logger.log(Level.WARNING, msg)

  //mapping custom exception to transport layer code for:
  //1. separation of concerns (not cluttering app logic code with transport layer error codes): so separating a reusable localAuthenticator
  //2. StatusRuntimeException swallows the error message and assigns a default status code UNKNOWN at client side.Failure(io.grpc.StatusRuntimeException: UNKNOWN)
  override def registerAsync(request: User): Future[RegisterResponse] = {
    log(msg = "al authservice (register) arrives: " + request)
    //se c'è problema nel local authenticator devo settare l'onError dell'observer (il cui equivalenti qui in scalapb è la future)
    //todo or delegate to external pool?
    mapThrowableByStatus(Future(
      localAuthenticator.register(request)
    ).map(_ => {
      RegisterResponse()
    }), {

      case _: BadContentException =>
        println("è un BadContentException ")

        Status.INVALID_ARGUMENT  //already wrapping message inside...
      case _: ConflictException =>
        println("è un conflict")
        Status.ALREADY_EXISTS
    })
  }

  override def authorizeAsync(request: Credentials): Future[Token] = {
    log(msg = "al authservice (authorize) arrives: " + request)

    mapThrowableByStatus(Future(
      localAuthenticator.authorize(request)
    ), {
      case _: BadContentException => Status.INVALID_ARGUMENT
      case _: WrongCredentialsException => Status.NOT_FOUND
    })
  }
}