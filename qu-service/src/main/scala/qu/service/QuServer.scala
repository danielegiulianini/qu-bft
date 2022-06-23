package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Grpc, InsecureChannelCredentials, InsecureServerCredentials, Server, ServerBuilder, ServerCredentials, ServerInterceptor}
import qu.{Shutdownable, Startable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
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


class QuServerImpl[Transportable[_], ObjectT](authorizationInterceptor: ServerInterceptor, //this makes it pluggable a different auth technology (possibly not JWT-based)
                                              credentials: ServerCredentials = InsecureServerCredentials.create(),
                                              quService: AbstractQuService[Transportable, ObjectT],
                                              port: Int)(implicit executor: ExecutionContext) extends QuServer {

  private val logger = Logger.getLogger(classOf[QuServerImpl[Transportable, ObjectT]].getName)

  private def log(level: Level = Level.INFO, msg: String) =
    logger.log(Level.INFO, msg)
println("prima di agtgiungee il service:::: " + quService)
  //here can plug creds with tls
  private val grpcServer = {
    println("jJJJJJJJJJJJJJJJJJJJJJJJJJJ quService is Hutdown?: " + quService.isShutdown)
    Grpc.newServerBuilderForPort(port,
      InsecureServerCredentials.create()) //ServerBuilder.forPort(port)
      .intercept(authorizationInterceptor)
      .addService(quService)
      .build
  }

  override def start(): Unit = {
    grpcServer.start
    log(msg = "server listening at port " + port + " started.")
  }

  override def shutdown(): Future[Unit] =
    for {
      _ <- quService.shutdown()
      _ <- Future {
        println("service shutdown!")
      }
      _ <- Future {
        grpcServer.shutdown
        grpcServer.awaitTermination()
        log(msg = "server shut down.together with servicd! (service shutdo?) "+ quService.isShutdown)
      }

      /*Future {
        grpcServer.shutdown
        grpcServer.awaitTermination()
        log(msg = "server shut down.")
      }.map(_ => quService.shutdown())*/
    } yield ()

  override def isShutdown: Flag = grpcServer.isShutdown
}


