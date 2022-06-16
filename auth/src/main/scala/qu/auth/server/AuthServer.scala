package qu.auth.server

import io.grpc.{Server, ServerBuilder}
import qu.auth.AuthGrpc

import scala.concurrent.{ExecutionContext, Future}

class AuthServer(ip: String, port: Int) /*extends Shutdownable*/ {

  self =>
  private[this] var server: Server = _

  def start()(implicit executionContext: ExecutionContext): Unit = {
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
      server.shutdown().awaitTermination()
    }
  }
}