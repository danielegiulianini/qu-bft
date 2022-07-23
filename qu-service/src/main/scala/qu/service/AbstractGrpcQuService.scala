package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.stub.ServerCalls
import io.grpc.{BindableService, MethodDescriptor, ServerCallHandler, ServerMethodDefinition, ServerServiceDefinition}
import presentation.MethodDescriptorFactory
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.SocketAddress.id
import qu.model.QuorumSystemThresholds
import qu.service.quorum.ServerQuorumPolicy.ServerQuorumPolicyFactory
import qu.{AbstractSocketAddress, JacksonMethodDescriptorFactory, SocketAddress, Shutdownable}
import qu.service.quorum.{JacksonBroadcastBroadcastServerPolicy, ServerQuorumPolicy}
import qu.storage.ImmutableStorage
import presentation.CachingMethodDescriptorFactory

import java.util.Objects
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._

import qu.model.ConcreteQuModel._

abstract class AbstractGrpcQuService[Transportable[_], ObjectT: TypeTag]()
                                                                        (implicit executor: ExecutionContext)
  extends BindableService with GrpcQuService[Transportable, ObjectT] with Shutdownable {

  //stuff that cannot be passed at creation time but must be provided for QuServiceImpl to work
  protected var ssd: ServerServiceDefinition = _
  protected var agnosticService: QuServiceImpl[Transportable, ObjectT] = _

  override def bindService(): ServerServiceDefinition = ssd

  override def shutdown(): Future[Unit] = agnosticService.shutdown()

  override def isShutdown: Flag = agnosticService.isShutdown

}


object AbstractGrpcQuService {

  //utility class for reducing number of parameters to pass
  case class ServerInfo(ip: String, port: Int, keySharedWithMe: String) extends AbstractSocketAddress

  case class QuServiceBuilder[Transportable[_], ObjectT: TypeTag](private val methodDescriptorFactory: presentation.MethodDescriptorFactory[Transportable],
                                                                  private val policyFactory: ServerQuorumPolicyFactory[Transportable, ObjectT],
                                                                  protected val thresholds: QuorumSystemThresholds,
                                                                  protected val ip: String,
                                                                  protected val port: Int,
                                                                  protected val privateKey: String,
                                                                  protected val obj: ObjectT,
                                                                  protected val storage: ImmutableStorage[ObjectT] = ImmutableStorage())
                                                                 (implicit ec: ExecutionContext) {

    private val quService = new GrpcQuServiceImpl[Transportable, ObjectT]()
    private val ssd = CachingServiceServerDefinitionBuilder(SERVICE_NAME)
    private var servers: Set[AbstractSocketAddress] = Set[AbstractSocketAddress]()
    private var keysSharedWithMe: Map[ServerId, Key] = Map[ServerId, String]()

    insertKeyForServer(id(SocketAddress(ip, port)), privateKey)

    //this precluded me the possibility of using scala's apply with name param as builder
    def addOperationOutput[OperationOutputT]()(implicit
                                               transportableRequest: Transportable[Request[OperationOutputT, ObjectT]],
                                               transportableResponse: Transportable[Response[Option[OperationOutputT]]],
                                               transportableLogicalTimestamp: Transportable[LogicalTimestamp],
                                               transportableObjectSyncResponse: Transportable[ObjectSyncResponse[ObjectT]],
                                               transportableObjectRequest: Transportable[Request[Object, ObjectT]],
                                               transportableObjectResponse: Transportable[Response[Option[Object]]],
                                               last: TypeTag[OperationOutputT]): QuServiceBuilder[Transportable, ObjectT] = {
      def addMethod[X: Transportable, Y: Transportable](handler: ServerCalls.UnaryMethod[X, Y]): Unit =
        ssd.addMethod(
          methodDescriptorFactory.generateMethodDescriptor[X, Y](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME),
          ServerCalls.asyncUnaryCall[X, Y](handler))


      addMethod[Request[OperationOutputT, ObjectT], Response[Option[OperationOutputT]]]((request, obs) => quService.sRequest(request, obs))
      addMethod[LogicalTimestamp, ObjectSyncResponse[ObjectT]](quService.sObjectRequest(_, _))
      //adding mds needed for handling barrier and copy requests
      addMethod[Request[Object, ObjectT], Response[Option[Object]]](quService.sRequest(_, _))
      this
    }

    def addServer(ip: String, port: Int, keySharedWithMe: String): QuServiceBuilder[Transportable, ObjectT] = {
      servers = servers + SocketAddress(ip, port)
      insertKeyForServer(id(ServerInfo(ip, port, keySharedWithMe)), keySharedWithMe)
      this
    }

    def addServer(serverInfo: ServerInfo): QuServiceBuilder[Transportable, ObjectT] = {
      Objects.requireNonNull(serverInfo) //require(serverInfo != null)
      insertKeyForServer(id(serverInfo), serverInfo.keySharedWithMe)
      addServer(serverInfo.ip, serverInfo.port, serverInfo.keySharedWithMe)
    }


    def addServers(serversInfo: Set[ServerInfo]): QuServiceBuilder[Transportable, ObjectT] = {
      for {
        server <- serversInfo
      } addServer(server)
      this
    }

    private def insertKeyForServer(id: ServerId, keySharedWithMe: Key): Unit = {
      keysSharedWithMe = keysSharedWithMe + (id -> keySharedWithMe)
    }

    def build(): AbstractGrpcQuService[Transportable, ObjectT] = {
      //validation
      quService.ssd = ssd.build()
      quService.agnosticService = new QuServiceImpl(ip, port, privateKey, obj, thresholds, storage, keysSharedWithMe, policyFactory(servers, thresholds))
      quService
    }
  }

  object QuServiceBuilder {

    def apply[Transportable[_], ObjectT: TypeTag](methodDescriptorFactory: presentation.MethodDescriptorFactory[Transportable],
                                                  policyFactory: ServerQuorumPolicyFactory[Transportable, ObjectT],
                                                  thresholds: QuorumSystemThresholds,
                                                  serverInfo: ServerInfo,
                                                  obj: ObjectT,
                                                  storage: ImmutableStorage[ObjectT])
                                                 (implicit ec: ExecutionContext): QuServiceBuilder[Transportable, ObjectT] = QuServiceBuilder(methodDescriptorFactory, policyFactory, thresholds, serverInfo.ip, serverInfo.port, serverInfo.keySharedWithMe, obj, storage)

    trait ServiceBuilderFactory[Transportable[_]] {
      def gen[ObjectT: TypeTag](serverInfo: ServerInfo,
                                thresholds: QuorumSystemThresholds,
                                obj: ObjectT)(implicit ec: ExecutionContext): QuServiceBuilder[Transportable, ObjectT]
    }

    class JacksonBroadcastServiceBuilderFactory extends ServiceBuilderFactory[JavaTypeable] {
      override def gen[ObjectT: TypeTag](serverInfo: ServerInfo,
                                         thresholds: QuorumSystemThresholds,
                                         obj: ObjectT)
                                        (implicit ec: ExecutionContext)
      : QuServiceBuilder[JavaTypeable, ObjectT] = {
        new QuServiceBuilder(
          methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable] {},
          policyFactory = JacksonBroadcastBroadcastServerPolicy[ObjectT](_, _),
          ip = serverInfo.ip,
          port = serverInfo.port,
          privateKey = serverInfo.keySharedWithMe,
          obj = obj,
          thresholds = thresholds,
          storage = ImmutableStorage[ObjectT]()
        )
      }
    }

  }
}
