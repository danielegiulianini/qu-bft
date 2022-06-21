package qu.auth.client

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import io.grpc.Status._
import qu.auth.AuthGrpc.AuthStub
import qu.auth.common.FutureUtilities.mapThrowable
import qu.auth._
import qu.auth.common.{AsyncAuthenticator, Authenticator, BadContentException, ConflictException, WrongCredentialsException}

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}


class ServiceException(cause: Throwable) extends Exception(cause: Throwable)

class AuthClient private(private val channel: ManagedChannel,
                         private val futureStub: AuthStub
                        )(implicit ec: ExecutionContext) extends Authenticator with AsyncAuthenticator /*extends Shutdownable*/ {

  private[this] val logger = Logger.getLogger(classOf[qu.auth.client.AuthClient].getName)

  def shutdown(): Future[Unit] = Future {
    channel.shutdown
    channel.awaitTermination(5, TimeUnit.SECONDS)
  }

  def isShutdown: Boolean = channel.isShutdown

  def registerAsync(name: String, password: String): Future[RegisterResponse] = {
    logger.info("Will try to register " + name + " ...")
    val request = User(username = name, password = password)
    mapThrowable(futureStub.registerAsync(request), {
      case error: StatusRuntimeException => error.getStatus match {
        case INVALID_ARGUMENT =>
          //logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          BadContentException(error.getMessage)
        case ALREADY_EXISTS =>
          logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
          println("arrivato al client il ALREADY_EXISTS ")
          ConflictException(error.getMessage)
        case _ if error.getCause.isInstanceOf[NullPointerException] =>
          BadContentException(error.getMessage)
        case _ => error
      }
      case th => th
    })
  }


  def authorizeAsync(username: String, password: String): Future[Token] = {
    logger.info("Will try to authirize " + username + " ...")
    mapThrowable(futureStub.authorizeAsync(Credentials(username, password)),
      {
        case error: StatusRuntimeException => error.getStatus match {
          case INVALID_ARGUMENT =>
            logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
            BadContentException(error.getMessage)
          case NOT_FOUND =>
            logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
            WrongCredentialsException(error.getMessage)
          case _ if error.getCause.isInstanceOf[NullPointerException] =>
            BadContentException(error.getMessage)
          case _ => error
        }
        case th => th
      })
  }

  override def register(user: User): Unit =
    await(registerAsync(user.username, user.password))

  override def authorize(credentials: Credentials): Token =
    await(authorizeAsync(credentials.username, credentials.password))

  //todo timeout
  protected def await[T](future: Future[T]): T = Await.result(future, 100.seconds)

  override def registerAsync(request: User): Future[RegisterResponse] = registerAsync(request.username, request.password)

  override def authorizeAsync(request: Credentials): Future[Token] = authorizeAsync(request.username, request.password)
}


object AuthClient {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): AuthClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel).withWaitForReady()
    new AuthClient(channel, asyncStub)
  }

  //factory for in-process channel (could have exposed an apply with channel (like private constructor)...
  //for easing in process construction too), as I want to instantiate it for testing but constructor is private...
  def apply(name: String)(implicit ec: ExecutionContext): AuthClient = {
    val channel = InProcessChannelBuilder.forName(name).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel).withWaitForReady()
    new AuthClient(channel, asyncStub)
  }
}



