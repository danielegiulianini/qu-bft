package qu.auth.server

import io.grpc.{Server, ServerBuilder}
import qu.auth.AuthGrpc
import qu.auth.client.AuthClient

import java.util.logging.{Level, Logger}
import scala.concurrent.duration.{DurationInt, MILLISECONDS, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Server providing JWT-token-based authorization and authentication facilities for users.
 * @param port the port for the server to listen on.
 */
class AuthServer(port: Int) /*extends Shutdownable*/ {

  self =>
  private[this] var server: Server = _

  private val logger = Logger.getLogger(classOf[AuthServer].getName)

  def start()(implicit executionContext: ExecutionContext): Unit = {
    logger.log(Level.INFO, "auth server starting...")
    server = ServerBuilder.forPort(port).addService(AuthGrpc.bindService(new AuthService, executionContext)).build.start
    /*sys.addShutdownHook {
      System.err.println("*** shutting down auth gRPC server since JVM is shutting down")
      //self.stop()
      self.shutdown()
      System.err.println("*** auth server shut down")
    }*/
  }

  def shutdown()(implicit executionContext: ExecutionContext): Future[Unit] = {
    Future {
      server.shutdown().awaitTermination(3, SECONDS)
      logger.log(Level.INFO, "server shut down.")
    }
  }
}

object AuthServer {
  def apply(port: Int)(implicit ec: ExecutionContext): AuthServer = new AuthServer(port)
}