package qu.auth.client

import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{ManagedChannel, ManagedChannelBuilder, StatusRuntimeException}
import qu.auth.AuthGrpc.AuthStub
import qu.auth.common.FutureUtilities.mapThrowable
import qu.auth._

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}
import scala.concurrent.Future

object AuthClient {
  def apply(host: String, port: Int): AuthClient = {
    val channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel)
    new AuthClient(channel, asyncStub)
  }

  //factory for in-process channel (could have exposed an apply with channel (like private constructor)...
  //for easing in process construction too), as I want to instantiate it for testing but constructor is private...
  def apply(name: String): AuthClient = {
    println("using apply of AuthClient")
    val channel = InProcessChannelBuilder.forName(name).usePlaintext().build
    val asyncStub = AuthGrpc.stub(channel)
    new AuthClient(channel, asyncStub)
  }

  /*def main(args: Array[String]): Unit = {
    val client = HelloWorldClient("localhost", 50051)
    try {
      val user = args.headOption.getOrElse("world")
      client.greet(user)
    } finally {
      client.shutdown()
    }
  }*/
}

class ServiceException(cause: Throwable) extends Exception(cause: Throwable)

class AuthClient private(
                          private val channel: ManagedChannel,
                          private val futureStub: AuthStub
                        ) {
  private[this] val logger = Logger.getLogger(classOf[AuthClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  //todo temporarily
  import scala.concurrent.ExecutionContext.Implicits.global //todo temporneous

  def register(name: String, password: String): Future[RegisterResponse] = {
    //async exception mapping
    logger.info("Will try to greet " + name + " ...")
    val request = User(username = name, password = password)
    mapThrowable(futureStub.register(request), { case error: StatusRuntimeException =>
      //probabile che qui non dica nulla ... al client... (infatti è così)... quindi posso già wrappare qualche exception...
      logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
      new ServiceException(error.getCause)
    })
  }

  def authorize(username: String, password: String): Future[Token] = {
    logger.info("Will try to authirize " + username + " ...")
    val request = Credentials(username = username, password = password)
    mapThrowable(futureStub.authorize(Credentials(username, password)),
      { case error: StatusRuntimeException =>
        //probabile che qui non dica nulla ... al client...
        logger.log(Level.WARNING, "RPC failed: {0}", error.getStatus)
        new ServiceException(error.getCause)
      })
  }
}
