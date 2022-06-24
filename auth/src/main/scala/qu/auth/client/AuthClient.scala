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
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 *  jwt token based authentication...//todo
 * @param channel
 * @param futureStub
 * @param maxTimeout
 * @param ec
 */
class AuthClient private(private val channel: ManagedChannel,
                         private val futureStub: AuthStub,
                         private val maxTimeout: Duration = 100.seconds
                        )(implicit ec: ExecutionContext) extends Authenticator with AsyncAuthenticator /*extends Shutdownable*/ {

  private[this] val logger = Logger.getLogger(classOf[qu.auth.client.AuthClient].getName)

  def shutdown(): Future[Unit] = Future {
    channel.shutdown
    channel.awaitTermination(5, TimeUnit.SECONDS)
  }

  def isShutdown: Boolean = channel.isShutdown

  def registerAsync(name: String, password: String): Future[RegisterResponse] = {
    logger.info("Auth server received register request from user " + name + ".")
    val request = User(username = name, password = password)
    mapThrowable(futureStub.registerAsync(request), {
      case error: StatusRuntimeException => error.getStatus match {
        case INVALID_ARGUMENT =>
          BadContentException(error.getMessage)
        case ALREADY_EXISTS =>
          ConflictException(error.getMessage)
        case _ if error.getCause.isInstanceOf[NullPointerException] =>
          BadContentException(error.getMessage)
        case _ => error
      }
      case th => th
    })
  }


  def authorizeAsync(username: String, password: String): Future[Token] = {
    logger.info("Auth server received authorize request from user " + username + ".")
    mapThrowable(futureStub.authorizeAsync(Credentials(username, password)),
      {
        case error: StatusRuntimeException => error.getStatus match {
          case INVALID_ARGUMENT =>
            BadContentException(error.getMessage)
          case NOT_FOUND =>
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

  protected def await[T](future: Future[T]): T = Await.result(future, maxTimeout)

  override def registerAsync(request: User): Future[RegisterResponse] = registerAsync(request.username, request.password)

  override def authorizeAsync(request: Credentials): Future[Token] = authorizeAsync(request.username, request.password)
}


object AuthClient {
  def apply(host: String, port: Int)(implicit ec: ExecutionContext): AuthClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel).withWaitForReady()
    new AuthClient(channel, asyncStub)
  }

  //factory for in-process channel
  def apply(name: String)(implicit ec: ExecutionContext): AuthClient = {
    val channel = InProcessChannelBuilder.forName(name).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel).withWaitForReady()
    new AuthClient(channel, asyncStub)
  }
}



