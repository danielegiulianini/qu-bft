package qu.model

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.SocketAddress.id
import qu.auth.server.AuthServer
import qu.client.datastructures.DistributedCounter
import qu.client.datastructures.Mode.NOT_REGISTERED
import qu.model.ConcreteQuModel.{Key, ServerId}
import qu.model.ServerStatus._
import qu.service.LocalQuServerCluster.buildServersFromRecipientInfoAndKeys
import qu.service.{LocalQuServerCluster, QuServer, QuServerBuilder}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
//import qu.service.ServersFixture

class SmrSystemImpl extends SmrSystem /*with ServersFixture*/ {

  val authServerInfo = SocketAddress(ip = "localhost", port = 1000)
  val quServer1 = SocketAddress(ip = "localhost", port = 1001)
  val quServer2 = SocketAddress(ip = "localhost", port = 1002)
  val quServer3 = SocketAddress(ip = "localhost", port = 1003)
  val quServer4 = SocketAddress(ip = "localhost", port = 1004)

  var quServerIpPorts = Set[SocketAddress]()
  quServerIpPorts = quServerIpPorts + quServer1
  quServerIpPorts = quServerIpPorts + quServer2
  quServerIpPorts = quServerIpPorts + quServer3
  quServerIpPorts = quServerIpPorts + quServer4

  val keysByServer: Map[ServerId, Map[ServerId, Key]] = Map(
    id(quServer1) -> Map(id(quServer1) -> "ks1s1",
      id(quServer2) -> "ks1s2",
      id(quServer3) -> "ks1s3",
      id(quServer4) -> "ks1s4"),
    id(quServer2) -> Map(id(quServer1) -> "ks1s2",
      id(quServer2) -> "ks2s2",
      id(quServer3) -> "ks2s3",
      id(quServer4) -> "ks2s4"),
    id(quServer3) -> Map(id(quServer1) -> "ks1s3",
      id(quServer2) -> "ks2s3",
      id(quServer3) -> "ks3s3",
      id(quServer4) -> "ks3s4"),
    id(quServer4) -> Map(id(quServer1) -> "ks1s4",
      id(quServer2) -> "ks2s4",
      id(quServer3) -> "ks3s4",
      id(quServer4) -> "ks4s4"))


  val FaultyServersCount = 1
  val MalevolentServersCount = 0
  val thresholds = QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount)

  val authServer = new AuthServer(authServerInfo.port)

  val cluster =
    LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      0)


  authServer.start()
  cluster.start()

  val distributedClient = new DistributedCounter(
    "username",
    "password",
    authServerInfo.ip,
    authServerInfo.port,
    quServerIpPorts,
    thresholds,
    NOT_REGISTERED)

  override def killServer(sid: ConcreteQuModel.ServerId): Try[ServerEventResult] = {

    if (!cluster.servers.contains(sid)) {
      Failure(ServerNotExistingException())
    }
    else if (cluster.servers(sid).isShutdown) {
      Failure(ServerAlreadyKilledException())
    }
    //if the shut down would exceed the maximum tolerated by the thresholds chosen must notify problem to user and prevent the killing
    //+ 1 is for the upcoming kill
    else if (cluster.servers.values.filter(_.isShutdown).size + 1 > thresholds.t) {
      Failure(ThresholdsExceededException())
    }
    else {
      Try {
        Await.ready(cluster.shutdownServer(sid), atMost = 5.seconds)
        ServerKilled(sid, getStatus)
      }
    }
  }

  override def increment(): Try[CounterEventResult] = Try {
    distributedClient.increment()
    IncResult
  }


  override def value(): Try[CounterEventResult] = Try {
    ValueResult(distributedClient.value())
  }


  override def reset(): Try[CounterEventResult] = Try {
    distributedClient.reset()
    ResetResult
  }

  override def stop(): Unit = {
    Await.ready(cluster.shutdown().map(_ => distributedClient.shutdown()), atMost = 5.seconds)
  }

  override def decrement(): Try[CounterEventResult] = Try {
    distributedClient.decrement()
    DecResult
  }

  override def getServersStatus: Try[ServerEventResult] = Try {
    ServersProfiled(getStatus)
  }

  private def getStatus: Map[ConcreteQuModel.ServerId, ServerStatus] =
    cluster.serversStatuses().view.mapValues(serverStatus => if (serverStatus) SHUTDOWN else ACTIVE).toMap
}


/*if model returning booleans instead of directly statuses:
override def killServer(sid: ConcreteQuModel.ServerId): Try[ServerEventResult] = Try {
    Await.ready(cluster.killServer(sid), atMost = 5.seconds)
    ServerKilled(sid, cluster.serversStatuses().view.mapValues(server => if (server) SHUTDOWN else ACTIVE).toMap)
  }
* */