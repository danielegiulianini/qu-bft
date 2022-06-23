package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.model.ConcreteQuModel.{Key, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}
import qu.{RecipientInfo, Shutdownable, Startable}

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

trait LocalQuServerCluster extends Startable with Shutdownable {

  def killServer(si: ServerId): Future[Unit]

  def serversStatuses(): Map[ServerId, Boolean]
}

class LocalQuServerClusterImpl(servers: Map[ServerId, QuServer])
                              (implicit ec: ExecutionContext)
  extends LocalQuServerCluster {

  private val logger = Logger.getLogger(classOf[LocalQuServerClusterImpl].getName)

  private def log(level: Level = Level.INFO, msg: String) =
    logger.log(Level.INFO, msg)

  override def start(): Unit = {
    log(msg = "local cluster starting...")
    servers.values.foreach(_.start())
  }

  override def shutdown(): Future[Unit] = {
    Future.reduce(servers.values.map(s => s.shutdown()))((_, _) => ()).map(_ => log(msg = "local cluster shut down.")
    )
  } //Future.sequence(servers.values.map(s => s.shutdown())) //servers.values.foreach(_.shutdown())

  override def isShutdown: Boolean = servers.values.forall(_.isShutdown)

  override def killServer(si: ConcreteQuModel.ServerId): Future[Unit] = servers(si).shutdown()

  override def serversStatuses(): Map[ConcreteQuModel.ServerId, Boolean] = servers.view.mapValues(_.isShutdown).toMap
}


object LocalQuServerCluster {

  //public factory methods
  def apply(servers: Map[ServerId, QuServer])(implicit ec: ExecutionContext) =
    new LocalQuServerClusterImpl(servers)

  def apply[T](quServerIpPorts: Set[RecipientInfo],
               keysByServer: Map[ServerId, Map[ServerId, Key]],
               thresholds: QuorumSystemThresholds,
               bl: ServerBuildingLogic[T],
               initialObj: T)(implicit ec: ExecutionContext): LocalQuServerClusterImpl = {
    println("l'ip ports: " + quServerIpPorts)
    println("le keys are: " + keysByServer)
    println("l'init obj is:  " + initialObj)

    LocalQuServerCluster(buildServersFromRecipientInfoAndKeys(quServerIpPorts,
      keysByServer,
      thresholds,
      bl,
      initialObj))
  }

  type ServerBuildingLogic[T] = (RecipientInfo, Key, QuorumSystemThresholds, T) => QuServerBuilder[JavaTypeable, T]

  //utilities
  def buildServersFromRecipientInfoAndKeys[T](quServerIpPorts: Set[RecipientInfo],
                                              keysByServer: Map[ServerId, Map[ServerId, Key]],
                                              thresholds: QuorumSystemThresholds,
                                              bl: ServerBuildingLogic[T],
                                              initialObj: T)(implicit ec: ExecutionContext) = {
    println("invoco : buildServersFromRecipientInfoAndKeys")
    def addServersToServer(ipPort: RecipientInfo) = {
      val serverBuilder = bl(ipPort, keysByServer(id(ipPort))(id(ipPort)), thresholds, initialObj)
      for {
        ipPort2 <- quServerIpPorts if ipPort2 != ipPort
      } serverBuilder.addServer(ipPort2.ip, ipPort2.port, keysByServer(id(ipPort))(id(ipPort2)))
      println("(LocalQuServerCluster) quildo il server" + ipPort)
      serverBuilder.build()
    }

    val servers = quServerIpPorts.map(ipPort => {
      println("(buildServersFromRecipientInfoAndKeys) aggiungo i servers al server " + id(ipPort))
      id(ipPort) -> addServersToServer(ipPort)
    }).toMap
    servers //new LocalQuServerClusterImpl(servers)
  }
}