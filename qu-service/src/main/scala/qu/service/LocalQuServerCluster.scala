package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress.id
import qu.model.ConcreteQuModel.{Key, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}
import qu.{SocketAddress, Shutdownable, Startable}

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

/**
 * A local Q/U cluster of replicas for local-execution of Q/U protocol.
 */
trait LocalQuServerCluster extends Startable with Shutdownable {

  val servers: Map[ServerId, QuServer]

  def shutdownServer(si: ServerId): Future[Unit]

  def serversStatuses(): Map[ServerId, Boolean]
}


/**
 *
 * @param servers
 */
class LocalQuServerClusterImpl(override val servers: Map[ServerId, QuServer])
                              (implicit ec: ExecutionContext)
  extends LocalQuServerCluster {

  import qu.LoggingUtils._
  private val logger = Logger.getLogger(classOf[LocalQuServerClusterImpl].getName)

  override def start(): Unit = {
    logger.log(Level.INFO, "local cluster starting...")
    servers.values.foreach(_.start())
  }

  override def shutdown(): Future[Unit] = {
    println("i server to shutdown are: " +servers.values.toList)
   // Future.traverse(servers.values.toList)(_.shutdown()).flatMap(_ => logger.logAsync(Level.INFO, "local cluster shut down."))
    def seqFutures[T, U](items: IterableOnce[T])(fun: T => Future[U])(implicit ec: ExecutionContext): Future[List[U]] = {
      items.iterator.foldLeft(Future.successful[List[U]](Nil)) {
        (f, item) =>
          f.flatMap {
            x => fun(item).map(_ :: x)
          }
      } map (_.reverse)
    }
    servers.values.toList.foldLeft(Future.unit)((fut, server) =>
        fut.flatMap(_ => server.shutdown())).map(r => println("shutdown all the servers"))
    //seqFutures(servers.values.toList)(_.shutdown()).map(a=>println(("custer shutodwn")))//.flatMap(_ => logger.logAsync(Level.INFO, "local cluster shut down."))
    //seqFutures(servers.values.toList)(_.shutdown()).map(a=>println(("custer shutodwn")))//.flatMap(_ => logger.logAsync(Level.INFO, "local cluster shut down."))

    /*Future.reduceLeft[Unit, Unit](servers.values.toList.map(_.shutdown())
    )((_, _) => ()).flatMap(_ => logger.logAsync(Level.INFO, "local cluster shut down."))*/
  }


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