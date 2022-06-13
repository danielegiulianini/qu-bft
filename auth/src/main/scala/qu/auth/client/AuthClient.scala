package qu.auth.client

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.grpc.Status._
import qu.auth.AuthGrpc.AuthStub
import qu.auth.common.FutureUtilities.mapThrowable
import qu.auth._
import qu.auth.common.{BadContentException, ConflictException, WrongCredentialsException}

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}


class ServiceException(cause: Throwable) extends Exception(cause: Throwable)

class AuthClient private(
                          private val channel: ManagedChannel,
                          private val futureStub: AuthStub
                        )(implicit ec: ExecutionContext) {
  private[this] val logger = Logger.getLogger(classOf[AuthClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def register(name: String, password: String): Future[RegisterResponse] = {
    logger.info("Will try to register " + name + " ...")
    val request = User(username = name, password = password)
    mapThrowable(futureStub.register(request), {
      case error: StatusRuntimeException => error.getStatus match {
        case INVALID_ARGUMENT =>
          //logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          throw BadContentException(error.getMessage)
        case ALREADY_EXISTS =>
          logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          throw ConflictException(error.getMessage)
      }
    })
  }

  def authorize(username: String, password: String): Future[Token] = {
    logger.info("Will try to authirize " + username + " ...")
    mapThrowable(futureStub.authorize(Credentials(username, password)),
      { case error: StatusRuntimeException => error.getStatus match {
        case INVALID_ARGUMENT =>
          //logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          throw BadContentException(error.getMessage)
        case NOT_FOUND =>
          logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          throw WrongCredentialsException(error.getMessage)
      }
      })
  }
}


object AuthClient {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): AuthClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel)
    new AuthClient(channel, asyncStub)
  }

  //factory for in-process channel (could have exposed an apply with channel (like private constructor)...
  //for easing in process construction too), as I want to instantiate it for testing but constructor is private...
  def apply(name: String)(implicit ec: ExecutionContext): AuthClient = {
    val channel = InProcessChannelBuilder.forName(name).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel)
    new AuthClient(channel, asyncStub)
  }
}



