package qu

import qu.client.datastructures.{Decrement, Increment, Reset, Value}


object ExampleCode extends App {

  import qu.model.QuorumSystemThresholds

  val thresholds = QuorumSystemThresholds(t = 2, b = 1)

  //auth server
  val authServerSocketAddr = SocketAddress("localhost", 1006)

  import scala.concurrent.ExecutionContext.Implicits.global
  import qu.auth.server.AuthServer

  val authServer = new AuthServer(authServerSocketAddr.port)
  authServer.start()


  //operations
  /*object Value extends QueryReturningObject[Int]

  case class Increment() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  case class Decrement() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj - 1
  }

  case class Reset() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = 0
  }*/


  //qu server

  import qu.service.QuServerBuilder
  import qu.service.AbstractQuService.ServerInfo

  val quReplica1 = ServerInfo(ip = "localhost", port = 1001, "ks1s1")
  val quReplica2 = ServerInfo(ip = "localhost", port = 1002, "ks1s2")
  //...
  val quReplica6 = ServerInfo(ip = "localhost", port = 1006, "ks1s6")

  val quServerBuilder = QuServerBuilder(quReplica1, thresholds, 0)
  quServerBuilder
    .addServer(quReplica2)
    //…
    .addServer(quReplica6)
    .addOperationOutput[Int]() //for Value
    .addOperationOutput[Unit]() //for Inc, Dec, Reset





  //client

  import qu.client.AuthenticatingClient

  val quServer1: SocketAddress = SocketAddress(ip = "localhost", port = 1000)
  val quServer2: SocketAddress = SocketAddress(ip = "localhost", port = 1001)
  //...
  val quServer6: SocketAddress = SocketAddress(ip = "localhost", port = 1003)

  val authClient = AuthenticatingClient[Int](
    authServerSocketAddr.ip,
    authServerSocketAddr.port,
    "username",
    "password")

  //async
  val authenticatedQuClient = for {
    _ <- authClient.register()
    builder <- authClient.authorize()
  } yield builder.addServer(quServer1)
    //...
    .addServer(quServer6)
    .withThresholds(thresholds).build

  for {
    authenticatedQuClient <- authenticatedQuClient
    _ <- authenticatedQuClient.submit[Unit](Increment())
    _ <- authenticatedQuClient.submit[Unit](Reset())
    _ <- authenticatedQuClient.submit[Unit](Increment())
    _ <- authenticatedQuClient.submit[Unit](Increment())
    _ <- authenticatedQuClient.submit[Unit](Decrement())
    value <- authenticatedQuClient.submit[Int](Value)
  } yield println("distributed counter value is:" + value)

  //sync

  import scala.concurrent.Await
  import scala.concurrent.duration.DurationInt

  Await.ready(authClient.register(), atMost = 1.seconds)
  val maxWait = 20.seconds
  val authenticatedSyncQuClient2 = Await.result(authClient.authorize(), atMost = 1.seconds)
  val authenticatedSyncQuClient = authenticatedSyncQuClient2.addServer(quServer1)
    //...
    .addServer(quServer6)
    .withThresholds(thresholds).build
  Await.ready(authenticatedSyncQuClient.submit(Increment()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Reset()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Increment()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Decrement()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Reset()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit[Int](Value), atMost = maxWait)

  val syncValue = Await.ready(authenticatedSyncQuClient.submit[Unit](Increment()), atMost = maxWait)
  println("distributed counter value is:" + syncValue)
}
