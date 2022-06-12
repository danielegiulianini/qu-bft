package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Server, ServerBuilder, ServerInterceptor}
import qu.{Shutdownable, Startable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.{ServerInfo, ServiceFactory, jacksonSimpleQuorumServiceFactory}
import qu.service.QuServerBuilder.jacksonSimpleServerBuilder

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._


//a facade that hides grpc internals
trait QuServer extends Startable with Shutdownable

//companion object
object QuServer {
  // creation by builder, not factory: def apply()

  //could use builder factory instead of defaultBuilder
  def builder[U: TypeTag](ip: String, port: Int, privateKey: String, thresholds: QuorumSystemThresholds, obj: U)(implicit executor: ExecutionContext): QuServerBuilder[JavaTypeable, U] =
    jacksonSimpleServerBuilder[U](ip, port, privateKey, thresholds, obj)
}



class QuServerImpl[Transportable[_], U](authorizationInterceptor: ServerInterceptor,
                                        quService: AbstractQuService[Transportable, U],
                                        port: Int)(implicit executor: ExecutionContext) extends QuServer {

  //here can plug creds with tls
  private val grpcServer = ServerBuilder
    .forPort(port)
    .intercept(authorizationInterceptor)
    .addService(quService)
    .build

  override def start(): Unit = grpcServer.start

  override def shutdown(): Future[Unit] = Future {
    //grpcServer.shutdown
    //val promise = Promise()
    //grpcServer.awaitTermination()
    //promise.future
    Future {
      grpcServer.shutdown
      grpcServer.awaitTermination()
    }
  }

  override def isShutdown: Flag = grpcServer.isShutdown
}

//alternative to apply in companion object
class QuServerBuilder[Transportable[_], ObjectT](private val serviceFactory: ServiceFactory[Transportable, ObjectT],
                                                 private val authorizationInterceptor: ServerInterceptor,
                                                 //user (injected) dependencies:
                                                 private val ip: String,
                                                 private val port: Int,
                                                 private val privateKey: String,
                                                 //private val
                                                 private val quorumSystemThresholds: QuorumSystemThresholds,
                                                 private val obj: ObjectT)(implicit executor: ExecutionContext) {

  private val quService: AbstractQuService[Transportable, ObjectT] =
    serviceFactory(ServerInfo(ip, port, privateKey), obj, quorumSystemThresholds)

  def addOperationOutput[T: TypeTag]()(implicit
                                       transportableRequest: Transportable[Request[T, ObjectT]],
                                       transportableResponse: Transportable[Response[Option[T]]],
                                       transportableLogicalTimestamp: Transportable[LogicalTimestamp],
                                       transportableObjectSyncResponse: Transportable[ObjectSyncResponse[ObjectT]],
                                       transportableObjectRequest: Transportable[Request[Object, ObjectT]],
                                       transportableObjectResponse: Transportable[Response[Option[Object]]]):
  QuServerBuilder[Transportable, ObjectT] = {
    quService.addOperationOutput[T]()
    this
  }

  def addServer(ip: String, port: Int, keySharedWithMe: String): QuServerBuilder[Transportable, ObjectT] = {
    quService.addServer(ip, port, keySharedWithMe)
    this
  }

  def build(): QuServer = {
    //todo validation missing
    new QuServerImpl(authorizationInterceptor, quService, port)
  } //this or: 1. factory of QUServer, 2. factory of QuServerImpl
}


object QuServerBuilder {
  //hided builder implementations (injecting dependencies)
  def jacksonSimpleServerBuilder[ObjectT: TypeTag](ip: String,
                                                   port: Int,
                                                   privateKey: String,
                                                   thresholds: QuorumSystemThresholds,
                                                   obj: ObjectT)(implicit ec: ExecutionContext) =
    new QuServerBuilder[JavaTypeable, ObjectT](
      jacksonSimpleQuorumServiceFactory(),
      new JwtAuthorizationServerInterceptor(),
      ip, port, privateKey,
      thresholds,
      obj)

}


/*
object QuServerBuilder {
  //hided builder implementations (injecting dependencies)
  def jacksonSimpleServerBuilder[U: TypeTag](myServerInfo: RecipientInfo,
                                             thresholds: QuorumSystemThresholds,
                                             obj: U,
                                             port: Int) =
    new QuServerBuilder[JavaTypeable, U](
      jacksonSimpleQuorumServiceFactory(),
      new JwtAuthorizationServerInterceptor(),
      myServerInfo,
      thresholds,
      obj,
      port)
*/
