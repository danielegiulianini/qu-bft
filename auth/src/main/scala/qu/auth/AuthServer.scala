package qu.auth

import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, Future}

class AuthServer(ip: String, port: Int) {

  self =>
  private[this] var server: Server = null

  def start()(implicit executionContext: ExecutionContext): Unit = {
    server = ServerBuilder.forPort(port).addService(AuthGrpc.bindService(new MyAuthService, executionContext)).build.start
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      //self.stop()
      self.shutdown()
      server.isShutdown
      System.err.println("*** server shut down")
    }
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
      println("cio")
      server.shutdown().awaitTermination()
    }
  }
}