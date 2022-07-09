package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress.id
import qu.model.ConcreteQuModel.{Key, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}
import qu.{SocketAddress, Shutdownable, Startable}

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

trait LocalQuServerCluster extends Startable with Shutdownable {

  val servers: Map[ServerId, QuServer]

  def shutdownServer(si: ServerId): Future[Unit]

  def serversStatuses(): Map[ServerId, Boolean]
}

class LocalQuServerClusterImpl(override val servers: Map[ServerId, QuServer])
                              (implicit ec: ExecutionContext)
  extends LocalQuServerCluster {

  private val logger = Logger.getLogger(classOf[LocalQuServerClusterImpl].getName)

  override def start(): Unit = {
    logger.log(Level.INFO, "local cluster starting...")
    servers.values.foreach(_.start())
  }

  override def shutdown(): Future[Unit] =
    Future.reduceLeft[Unit, Unit](servers.values.toList.map(_.shutdown())
    )((_, _) => ()).map(_ => logger.log(Level.INFO, "local cluster shut down."))


  override def isShutdown: Boolean = servers.values.forall(_.isShutdown)

  override def shutdownServer(si: ConcreteQuModel.ServerId): Future[Unit] = {
    servers(si).shutdown()
  }

  override def serversStatuses(): Map[ConcreteQuModel.ServerId, Boolean] = servers.view.mapValues(_.isShutdown).toMap

}


object LocalQuServerCluster {

  //public factory methods
  def apply(servers: Map[ServerId, QuServer])(implicit ec: ExecutionContext) =
    new LocalQuServerClusterImpl(servers)

  def apply[T](quServerIpPorts: Set[SocketAddress],
               keysByServer: Map[ServerId, Map[ServerId, Key]],
               thresholds: QuorumSystemThresholds,
               bl: ServerBuildingLogic[T],
               initialObj: T)(implicit ec: ExecutionContext): LocalQuServerClusterImpl = {
    LocalQuServerCluster(buildServersFromRecipientInfoAndKeys(quServerIpPorts,
      keysByServer,
      thresholds,
      bl,
      initialObj))
  }

  type ServerBuildingLogic[T] = (SocketAddress, Key, QuorumSystemThresholds, T) => QuServerBuilder[JavaTypeable, T]

  //utilities
  def buildServersFromRecipientInfoAndKeys[T](quServerIpPorts: Set[SocketAddress],
                                              keysByServer: Map[ServerId, Map[ServerId, Key]],
                                              thresholds: QuorumSystemThresholds,
                                              bl: ServerBuildingLogic[T],
                                              initialObj: T)(implicit ec: ExecutionContext): Map[Key, QuServer] = {
    def addServersToServer(ipPort: SocketAddress) = {
      val serverBuilder = bl(ipPort, keysByServer(id(ipPort))(id(ipPort)), thresholds, initialObj)
      for {
        ipPort2 <- quServerIpPorts if ipPort2 != ipPort
      } serverBuilder.addServer(ipPort2.ip, ipPort2.port, keysByServer(id(ipPort))(id(ipPort2)))
      serverBuilder.build()
    }

    val servers = quServerIpPorts.map(ipPort => {
      id(ipPort) -> addServersToServer(ipPort)
    }).toMap
    servers
  }
}