package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ServerInterceptor
import qu.auth.Token
import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, Request, Response}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.QuServiceBuilder.ServiceBuilderFactory
import qu.service.AbstractQuService.{QuServiceBuilder, ServerInfo}

import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

//alternative to QuServer apply in companion object
case class QuServerBuilder[Transportable[_], ObjectT: TypeTag](
                                                                //programmer (injected dependencies):
                                                                private val serviceBuilderFactory: ServiceBuilderFactory[Transportable],
                                                                private val authorizationInterceptor: ServerInterceptor,
                                                                //user (injected) dependencies:
                                                                private val ip: String,
                                                                private val port: Int,
                                                                private val privateKey: String,
                                                                private val quorumSystemThresholds: QuorumSystemThresholds,
                                                                private val obj: ObjectT)
                                                              (implicit executor: ExecutionContext) {


  private val serviceBuilder: QuServiceBuilder[Transportable, ObjectT] =
    serviceBuilderFactory.gen[ObjectT](ServerInfo(ip, port, privateKey), quorumSystemThresholds, obj)

  def addOperationOutput[T: TypeTag]()(implicit
                                       transportableRequest: Transportable[Request[T, ObjectT]],
                                       transportableResponse: Transportable[Response[Option[T]]],
                                       transportableLogicalTimestamp: Transportable[LogicalTimestamp],
                                       transportableObjectSyncResponse: Transportable[ObjectSyncResponse[ObjectT]],
                                       transportableObjectRequest: Transportable[Request[Object, ObjectT]],
                                       transportableObjectResponse: Transportable[Response[Option[Object]]]):
  QuServerBuilder[Transportable, ObjectT] = {
    serviceBuilder.addOperationOutput[T]()
    this
  }

  def addServer(ip: String, port: Int, keySharedWithMe: String): QuServerBuilder[Transportable, ObjectT] = {
    serviceBuilder.addServer(ip, port, keySharedWithMe)
    this
  }

  def addServer(serverInfo: ServerInfo): QuServerBuilder[Transportable, ObjectT] = {
    serviceBuilder.addServer(serverInfo)
    this
  }

  def build(): QuServer =
  //todo validation missing
    new QuServerImpl(authorizationInterceptor,
      quService = serviceBuilder.build(),
      port = port)
}

object QuServerBuilder {

  //choosing an implementation as the default (hiding programmer dependencies!)
  def apply[U: TypeTag](ip: String,
                        port: Int,
                        privateKey: String,
                        thresholds: QuorumSystemThresholds,
                        obj: U)(implicit ec: ExecutionContext): QuServerBuilder[JavaTypeable, U] =
    new JacksonServerBuilderFactory().simpleBroadcastServerBuilder[U](ip = ip,
      port = port,
      privateKey = privateKey,
      thresholds = thresholds,
      obj = obj)

  def apply[U: TypeTag](serverInfo: ServerInfo,
                        thresholds: QuorumSystemThresholds,
                        obj: U)(implicit ec: ExecutionContext): QuServerBuilder[JavaTypeable, U] =
    QuServerBuilder(
      serverInfo.ip,
      serverInfo.port,
      serverInfo.keySharedWithMe,
      thresholds = thresholds,
      obj = obj)
}
