package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ServerInterceptor
import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, Request, Response}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.{ServerInfo, ServiceFactory, jacksonSimpleQuorumServiceFactory}
import scala.reflect.runtime.universe._

import scala.concurrent.ExecutionContext

//alternative to QuServer apply in companion object
class QuServerBuilder[Transportable[_], ObjectT](private val serviceFactory: ServiceFactory[Transportable, ObjectT],
                                                 private val authorizationInterceptor: ServerInterceptor,
                                                 //user (injected) dependencies:
                                                 private val ip: String,
                                                 private val port: Int,
                                                 private val privateKey: String,
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
      ip,
      port,
      privateKey,
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