package qu.model

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo
import qu.RecipientInfo.id
import qu.auth.server.AuthServer
import qu.client.datastructures.DistributedCounter
import qu.model.ConcreteQuModel.{Key, ServerId}
import qu.model.ServerStatus._
import qu.service.LocalQuServerCluster.buildServersFromRecipientInfoAndKeys
import qu.service.{LocalQuServerCluster, QuServer, QuServerBuilder}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


class SyncSmrSystemImpl extends SyncSmrSystem {

  val authServerInfo = RecipientInfo(ip = "localhost", port = 1)
  val quServer1 = RecipientInfo(ip = "localhost", port = 1)
  val quServer2 = RecipientInfo(ip = "localhost", port = 2)
  val quServer3 = RecipientInfo(ip = "localhost", port = 3)
  val quServer4 = RecipientInfo(ip = "localhost", port = 4)

  var quServerIpPorts = Set[RecipientInfo]()
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

  val authServer = new AuthServer(authServerInfo.ip, authServerInfo.port)

  val cluster =
    LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      0)

  //todo must be started...

  val distributedClient = new DistributedCounter(
    "username",
    "password",
    authServerInfo.ip,
    authServerInfo.port,
    quServerIpPorts,
    thresholds)

  override def killServer(sid: ConcreteQuModel.ServerId): Try[ServerEventResult] = Try {
    Await.ready(cluster.killServer(sid), atMost = 1.seconds)
    ServerKilled(sid, cluster.serversStatuses().view.mapValues(server => if (server) SHUTDOWN else ACTIVE).toMap)
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
    //Future.reduce(servers.values.map(s => s.shutdown()))((_, _) => ())
    cluster.shutdown()
  }

  override def decrement(): Try[CounterEventResult] = Try {
    distributedClient.decrement()
    DecResult
  }
}



//creating servers automatically from keys
/*private def buildServersFromRecipientInfoAndKeys(quServerIpPorts: Set[RecipientInfo], keysByServer: Map[ServerId, Map[ServerId, Key]]) = {
  val servers = quServerIpPorts.map(ipPort => id(ipPort) -> {
    val server = RemoteCounterServer.builder(ipPort.ip,
      ipPort.port,
      keysByServer(id(ipPort))(id(ipPort)),
      thresholds = thresholds) //ipPorts.filterNot(_==ipPort)
    for {
      ipPort2 <- quServerIpPorts if ipPort2 != ipPort
    } server.addServer(ipPort2.ip, ipPort2.port, keysByServer(id(ipPort))(id(ipPort2)))
    server.build()
  }).toMap
  servers
}*/


/*var lastOperation: Future[_] = ???
def value2(): Future[Unit] = {
    this.synchronized {
  var lastOperation2: Future[Unit] = lastOperation.map(op => distributedClient.valueAsync)
  }
  lastOperation = lastOperation2
  distributedClient.value()
  lastOperation2
}*/
