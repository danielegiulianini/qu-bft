package qu.auth

import io.grpc.{Server, ServerBuilder}

import scala.concurrent.ExecutionContext

class AuthServer(ip: String, port: Int) {

  self =>
  private[this] var server: Server = null

  def start()(implicit executionContext: ExecutionContext): Unit = {
    server = ServerBuilder.forPort(port).addService(AuthGrpc.bindService(new MyAuthService, executionContext)).build.start
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}