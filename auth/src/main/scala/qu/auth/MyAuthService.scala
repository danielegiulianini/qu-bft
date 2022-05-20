package qu.auth

import io.grpc.Status

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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

  private def mapThrowableByStatus[T](f: Future[T], f2: Throwable => Status) : Future[T] = f transform {
    mapThrowable(f, error => f2(error).withDescription(error.getMessage()).withCause(error)
      .asException())
  }

  private def mapThrowable[T](f: Future[T], exFact: Throwable => Throwable) = f transform {
    case s@Success(_) => s
    case Failure(cause) => Failure(exFact(cause))
  }


  //TODO reusable, general utilities: could be plugged with implicit conversion on Future type
  private def mapException[T](f: Future[T], ex: Exception) = f transform {
    case s@Success(_) => s
    case Failure(cause) => {
      Failure(ex)
    }
  }


}
