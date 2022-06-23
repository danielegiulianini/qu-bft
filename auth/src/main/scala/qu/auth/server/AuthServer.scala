package qu.auth.server

import io.grpc.{Server, ServerBuilder}
import qu.auth.AuthGrpc
import qu.auth.client.AuthClient

import scala.concurrent.duration.{DurationInt, MILLISECONDS, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class AuthServer(port: Int) /*extends Shutdownable*/ {

  self =>
  private[this] var server: Server = _

  def start()(implicit executionContext: ExecutionContext): Unit = {
    println("auth server starting !!!!")
    server = ServerBuilder.forPort(port).addService(AuthGrpc.bindService(new MyAuthService, executionContext)).build.start
    /*sys.addShutdownHook {
      System.err.println("*** shutting down auth gRPC server since JVM is shutting down")
      //self.stop()
      self.shutdown()
      System.err.println("*** auth server shut down")
    }*/
  }

  /*def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }*/

  def shutdown()(implicit executionContext: ExecutionContext): Future[Unit] = {
    Future {
      println("shutting down...")
      println("auth server shutdown")
      server.shutdown().awaitTermination(3, SECONDS)
    }

    /*server.shutdown().awaitTermination(3, SECONDS)
    Future.unit*/
  }
}

object AuthServer {
  def apply(port: Int)(implicit ec: ExecutionContext): AuthServer = new AuthServer(port)
}