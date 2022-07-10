package qu

import qu.client.datastructures.{Decrement, Increment, Reset, Value}

//code posted on README.md. Put it here to leverage compile check.
object UsageExampleCode extends App {

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

  val quReplica1Info = ServerInfo(ip = "localhost", port = 1001, "ks1s1")
  val quReplica2Info = ServerInfo(ip = "localhost", port = 1002, "ks1s2")
  //...
  val quReplica6Info = ServerInfo(ip = "localhost", port = 1006, "ks1s6")

  val quServer = QuServerBuilder(quReplica1Info, thresholds, 0)
    .addServer(quReplica2Info)
    //…
    .addServer(quReplica6Info)
    .addOperationOutput[Int]() //for Value
    .addOperationOutput[Unit]() //for Inc, Dec, Reset
    .build

  quServer.start()

  //client

  import qu.client.AuthenticatingClient

  val quServer1SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1000)
  val quServer2SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1001)
  //...
  val quServer6SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1003)

  val authClient = AuthenticatingClient[Int](
    authServerSocketAddr.ip,
    authServerSocketAddr.port,
    "username",
    "password")

  //async
  val authenticatedQuClientBuilder = for {
    _ <- authClient.register()
    builder <- authClient.authorize()
  } yield builder

  val authenticatedQuClient =
    for {
      builder <- authenticatedQuClientBuilder
    }
    yield builder.addServer(quServer1SocketAddr)
      //...
      .addServer(quServer6SocketAddr)
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

  //client shutdown
  authClient.shutdown()
  authenticatedQuClient.map(_.shutdown())


  //sync

  import scala.concurrent.Await
  import scala.concurrent.duration.DurationInt

  Await.ready(authClient.register(), atMost = 1.seconds)
  val maxWait = 30.seconds
  val quClientBuilder = Await.result(authClient.authorize(), atMost = 1.seconds)
  val authenticatedSyncQuClient = quClientBuilder.addServer(quServer1SocketAddr)
    //...
    .addServer(quServer6SocketAddr)
    .withThresholds(thresholds).build
  Await.ready(authenticatedSyncQuClient.submit(Increment()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Reset()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Increment()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Decrement()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit(Reset()), atMost = maxWait)
  Await.ready(authenticatedSyncQuClient.submit[Int](Value), atMost = maxWait)

  val syncValue = Await.ready(authenticatedSyncQuClient.submit[Unit](Increment()), atMost = maxWait)
  println("distributed counter value is:" + syncValue)

  Await.ready(authClient.shutdown(), 1.seconds)
  Await.ready(authenticatedSyncQuClient.shutdown(), 1.seconds)

  //auth server and replicas shutdown
  authServer.shutdown()
  quServer.shutdown()

}
