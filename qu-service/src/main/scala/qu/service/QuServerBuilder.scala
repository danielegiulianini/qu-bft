package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ServerInterceptor
import qu.auth.Token
import qu.model.QuorumSystemThresholdQuModel.{LogicalTimestamp, ObjectSyncResponse, Request, Response}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractGrpcQuService.QuServiceBuilder.ServiceBuilderFactory
import qu.service.AbstractGrpcQuService.{QuServiceBuilder, ServerInfo}

import scala.reflect.runtime.universe._
import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

/**
 * A (GoF) builder for [[qu.service.QuServer]] instances.
 * @param serviceBuilderFactory the factory of Q/U service to inject into the replica under construction.
 * @param authorizationInterceptor the interceptor responsible for authorization check for the replica under
 *                                 construction
 * @param ip the ip the replica under construction will be listening on.
 * @param port the ip the replica under construction will be listening on.
 * @param privateKey the secret key used by this replica to generate authenticators for its Replica History
 *                   integrity check.
 * @param quorumSystemThresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param obj the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam ObjectT the type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
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
    new QuServerImpl(authorizationInterceptor,
      quService = serviceBuilder.build(),
      port = port)
}

object QuServerBuilder {

  //(GoF) factory method choosing an implementation as the default (hiding programmer dependencies)
  def apply[ObjectT: TypeTag](ip: String,
                              port: Int,
                              privateKey: String,
                              thresholds: QuorumSystemThresholds,
                              obj: ObjectT)(implicit ec: ExecutionContext): QuServerBuilder[JavaTypeable, ObjectT] =
    new JacksonServerBuilderFactory().simpleBroadcastServerBuilder[ObjectT](ip = ip,
      port = port,
      privateKey = privateKey,
      thresholds = thresholds,
      obj = obj)

  //(GoF) factory method reducing arguments required by leveraging ServerInfo.
  def apply[ObjectT: TypeTag](serverInfo: ServerInfo,
                              thresholds: QuorumSystemThresholds,
                              obj: ObjectT)(implicit ec: ExecutionContext): QuServerBuilder[JavaTypeable, ObjectT] =
    QuServerBuilder(
      serverInfo.ip,
      serverInfo.port,
      serverInfo.keySharedWithMe,
      thresholds = thresholds,
      obj = obj)
}
