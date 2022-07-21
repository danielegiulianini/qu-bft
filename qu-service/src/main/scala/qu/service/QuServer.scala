package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Grpc, InsecureChannelCredentials, InsecureServerCredentials, Server, ServerBuilder, ServerCredentials, ServerInterceptor}
import qu.{Shutdownable, Startable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

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
    QuServerBuilder[U](ip, port, privateKey, thresholds, obj)
}


class QuServerImpl[Transportable[_], ObjectT](authorizationInterceptor: ServerInterceptor, //this makes it pluggable a different auth technology (possibly not JWT-based)
                                              credentials: ServerCredentials = InsecureServerCredentials.create(),
                                              quService: AbstractQuService2[Transportable, ObjectT],
                                              port: Int)(implicit executor: ExecutionContext) extends QuServer {

  private val logger = Logger.getLogger(classOf[QuServerImpl[Transportable, ObjectT]].getName)

  private val grpcServer =
    Grpc.newServerBuilderForPort(port,
      credentials) //ServerBuilder.forPort(port)
      .intercept(authorizationInterceptor)
      .addService(quService)
      .build

  override def start(): Unit = {
    grpcServer.start
    logger.log(Level.INFO, "server listening at port " + port + " started.")
  }

  override def shutdown(): Future[Unit] =
    for {
      _ <- quService.shutdown()
      _ <- Future {
        grpcServer.shutdown
        grpcServer.awaitTermination()
        logger.log(Level.INFO, "server shut down.")
      }

    } yield ()

  override def isShutdown: Flag = grpcServer.isShutdown
}


