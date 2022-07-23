package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Grpc, InsecureChannelCredentials, InsecureServerCredentials, Server, ServerBuilder, ServerCredentials, ServerInterceptor}
import qu.{Shutdownable, Startable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

/**
 * A deployable Q/U replica containing the Q/U service-side logic.
**/
trait QuServer extends Startable with Shutdownable


object QuServer {

  def builder[U: TypeTag](ip: String, port: Int, privateKey: String,
                          thresholds: QuorumSystemThresholds,
                          obj: U)
                         (implicit executor: ExecutionContext): QuServerBuilder[JavaTypeable, U] =
    QuServerBuilder[U](ip, port, privateKey, thresholds, obj)
}

/**
 * An implementation of a [[qu.service.QuServer]] including repeated requests, inline repairing, compact timestamp,
 * pruning of replica histories and optimistic query execution optimizations. It is compatible (i.e. reusable)
 * with different (de)serialization, authentication technologies and with different object-syncing policies.
 * It is realized as a (GoF) facade that hides gRPC internals.
 */
class QuServerImpl[Transportable[_], ObjectT](authorizationInterceptor: ServerInterceptor, //this makes it pluggable a different auth technology (possibly not JWT-based)
                                              credentials: ServerCredentials = InsecureServerCredentials.create(),
                                              quService: AbstractGrpcQuService[Transportable, ObjectT],
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
      }
      _ <- Future {
        grpcServer.awaitTermination()
      }
      _ <- Future {
        logger.log(Level.INFO, "server shut down.")
      }
    } yield ()

  override def isShutdown: Flag = grpcServer.isShutdown
}


