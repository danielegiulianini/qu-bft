package qu

import qu.client.datastructures.DistributedCounter
import qu.service.AbstractGrpcQuService.ServerInfo
import qu.service.datastructures.RemoteCounterServer

object UsageExampleCodeDataStructure extends App {

  import qu.model.QuorumSystemThresholds

  val thresholds = QuorumSystemThresholds(t = 2, b = 1)

  //auth server
  val authServerSocketAddr = SocketAddress("localhost", 1006)

  import qu.auth.server.AuthServer

  import scala.concurrent.ExecutionContext.Implicits.global

  val authServer = new AuthServer(authServerSocketAddr.port)
  authServer.start()


  val quServer1SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1000)
  val quServer2SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1001)
  //...
  val quServer6SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1003)


  //client configuration
  val quServersInfo = Set(quServer1SocketAddr, quServer2SocketAddr /*, ...*/ , quServer6SocketAddr)
  val distributedCounter = DistributedCounter("username",
    "password", authServerSocketAddr.ip, authServerSocketAddr.port, quServersInfo, thresholds)

  //operations submission
  for {
    _ <- distributedCounter.incrementAsync()
    _ <- distributedCounter.resetAsync()
    _ <- distributedCounter.incrementAsync()
    _ <- distributedCounter.incrementAsync()
    _ <- distributedCounter.decrementAsync()
    value <- distributedCounter.valueAsync
  } yield println("distributed counter value is:" + value)

  //replica configuration
  val quReplica1Info = ServerInfo(ip = "localhost", port = 1001, "ks1s1")
  val quReplica2Info = ServerInfo(ip = "localhost", port = 1002, "ks1s2")
  //...
  val quReplica6Info = ServerInfo(ip = "localhost", port = 1006, "ks1s6")


  RemoteCounterServer.builder(quReplica1Info.ip, quReplica1Info.port, quReplica1Info.keySharedWithMe, thresholds)
    .addServer(quReplica2Info)
    //…
    .addServer(quReplica6Info)
    .build()
}
