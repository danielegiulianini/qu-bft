package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{ServerBuilder, ServerInterceptor}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.{ServerInfo, ServiceFactory, jacksonSimpleQuorumServiceFactory}
import qu.service.QuServerBuilder.jacksonSimpleServerBuilder

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._


//a facade that hides grpc internals
trait QuServer {

  def start(): Unit

  def stop()(implicit executor: ExecutionContext): Future[Unit]
}

//companion object
object QuServer {
  // creation by builder, not factory: def apply()

  //could use builder factory instead of defaultBuilder
  def builder[U: TypeTag](ip: String, port: Int, privateKey:String, thresholds: QuorumSystemThresholds, obj: U): Unit =
    jacksonSimpleServerBuilder[U](ip, port, privateKey, thresholds, obj)
}


//todo must inject dependency on:
// - interceptor (call authentication)... (one field more), and for
// - inprocess (fixture)? another dependencies (or could be the same if abstracting all in one)
// - tls support
class QuServerImpl[Marshallable[_], U](authorizationInterceptor: ServerInterceptor,
                                       quService: AbstractQuService[Marshallable, U],
                                       port: Int) extends QuServer {

  //here can plug creds with tls
  private val grpcServer = ServerBuilder
    .forPort(port)
    .intercept(authorizationInterceptor)
    .addService(quService)
    .build

  override def start(): Unit = grpcServer.start

  override def stop()(implicit executor: ExecutionContext): Future[Unit] = Future {
    grpcServer.shutdown
    //val promise = Promise()
    grpcServer.awaitTermination()
    //promise.future
  }
}

//alternative to apply in companion object
class QuServerBuilder[Marshallable[_], U](private val serviceFactory: ServiceFactory[Marshallable, U],
                                          private val authorizationInterceptor: ServerInterceptor,
                                          //user (injected) dependencies:
                                          private val ip: String,
                                          private val port: Int,
                                          private val privateKey: String,
                                          //private val
                                          private val quorumSystemThresholds: QuorumSystemThresholds,
                                          private val obj: U) {

  private val quService: AbstractQuService[Marshallable, U] = serviceFactory(ServerInfo(ip, port,privateKey), obj, quorumSystemThresholds)

  def addOperation[T: TypeTag](implicit
                               marshallableRequest: Marshallable[Request[T, U]],
                               marshallableResponse: Marshallable[Response[Option[T]]],
                               marshallableLogicalTimestamp: Marshallable[LogicalTimestamp],
                               marshallableObjectSyncResponse: Marshallable[ObjectSyncResponse[U]]):
    QuServerBuilder[Marshallable, U] = {
    quService.addOp[T]()
    this
  }

  def addServer(ip: String, port: Int, keySharedWithMe: String): QuServerBuilder[Marshallable, U] = {
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
  def jacksonSimpleServerBuilder[U: TypeTag](ip: String,
                                             port: Int, privateKey: String,
                                             thresholds: QuorumSystemThresholds,
                                             obj: U) =
    new QuServerBuilder[JavaTypeable, U](
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

}
*/
