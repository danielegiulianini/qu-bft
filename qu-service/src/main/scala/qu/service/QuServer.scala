package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Server, ServerBuilder, ServerInterceptor}
import qu.auth.server.JwtAuthorizationServerInterceptor
import qu.{Shutdownable, Startable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.{ServerInfo, ServiceFactory, jacksonSimpleQuorumServiceFactory}
import qu.service.QuServerBuilder.jacksonSimpleServerBuilder

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._


//a facade that hides grpc internals
trait QuServer extends Startable with Shutdownable

//companion object
object QuServer {
  // creation by builder, not factory: def apply()

  //could use builder factory instead of defaultBuilder
  def builder[U: TypeTag](ip: String, port: Int, privateKey: String,
                          thresholds: QuorumSystemThresholds,
                          obj: U)
                         (implicit executor: ExecutionContext): QuServerBuilder[JavaTypeable, U] =
    jacksonSimpleServerBuilder[U](ip, port, privateKey, thresholds, obj)
}


class QuServerImpl[Transportable[_], ObjectT](authorizationInterceptor: ServerInterceptor,
                                              quService: AbstractQuService[Transportable, ObjectT],
                                              port: Int)(implicit executor: ExecutionContext) extends QuServer {

  private val logger = Logger.getLogger(classOf[QuServerImpl[Transportable, ObjectT]].getName)

  private def log(level: Level = Level.INFO, msg: String) =
    logger.log(Level.INFO, msg)

  //here can plug creds with tls
  private val grpcServer = ServerBuilder
    .forPort(port)
    .intercept(authorizationInterceptor)
    .addService(quService)
    .build

  override def start(): Unit = {
    grpcServer.start
    log(msg = "server listening at port " + port + " started.")
  }

  override def shutdown(): Future[Unit] = Future {
    //grpcServer.shutdown
    //val promise = Promise()
    //grpcServer.awaitTermination()
    //promise.future
    Future {
      grpcServer.shutdown
      grpcServer.awaitTermination()
      log(msg = "server shut down.")
    }
  }

  override def isShutdown: Flag = grpcServer.isShutdown
}


