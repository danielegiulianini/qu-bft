package qu.model

import qu.SocketAddress
import qu.SocketAddress.id
import qu.auth.server.AuthServer
import qu.client.datastructures.DistributedCounter
import qu.client.datastructures.Mode.NOT_REGISTERED
import qu.model.QuorumSystemThresholdQuModel.{Key, ServerId}
import qu.model.ServerStatus._
import qu.service.datastructures.RemoteCounterServer
import qu.service.{LocalQuServerCluster, LocalQuServerClusterImpl}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Try}


/**
 * An implementation of [[SmrSystem]] based on a local cluster of Q/U replicas.
 */
class SmrSystemImpl extends SmrSystem {

  val authServerInfo: SocketAddress = SocketAddress(ip = "localhost", port = 1000)
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

  val cluster: LocalQuServerClusterImpl =
    LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      0)


  authServer.start()
  cluster.start()

  val distributedClient = DistributedCounter(
    "username",
    "password",
    authServerInfo.ip,
    authServerInfo.port,
    quServerIpPorts,
    thresholds,
    NOT_REGISTERED)

  override def killServer(sid: QuorumSystemThresholdQuModel.ServerId): Try[ServerEventResult] = {

    if (!cluster.servers.contains(sid)) {
      Failure(ServerNotExistingException())
    }
    else if (cluster.servers(sid).isShutdown) {
      Failure(ServerAlreadyKilledException())
    }
    //if the shut down would exceed the maximum tolerated by the thresholds chosen then
    // must notify problem to user and prevent the killing
    else if (cluster.servers.values.count(_.isShutdown) + 1 > thresholds.t) {    //+ 1 is for the upcoming kill
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

  private def getStatus: Map[QuorumSystemThresholdQuModel.ServerId, ServerStatus] =
    cluster.serversStatuses().view.mapValues(serverStatus => if (serverStatus) SHUTDOWN else ACTIVE).toMap
}
