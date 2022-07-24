package qu.service

import io.grpc.{BindableService, Context, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import presentation.MethodDescriptorFactory
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.SocketAddress.id
import qu.auth.common.Constants
import qu.{AbstractSocketAddress, Shutdownable, SocketAddress}
import qu.model.QuorumSystemThresholdQuModel.hmac
import qu.model.{QuorumSystemThresholdQuModel, QuorumSystemThresholds, StatusCode}
//import qu.service.AbstractQuService.ServerInfo
import qu.service.quorum.ServerQuorumPolicy
import qu.service.quorum.ServerQuorumPolicy.ServerQuorumPolicyFactory
import qu.storage.{ImmutableStorage, Storage}

import java.util.Objects
import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.math.Ordered.orderingToOrdered
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

//import that declares specific dependency
import qu.model.QuorumSystemThresholdQuModel._

/**
 * A gPRC implementation of a Q/U service, compatible with different (de)serialization, authentication technologies
 * and with different object-syncing policies.
 * @tparam ObjectT the type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
class GrpcQuServiceImpl[Transportable[_], ObjectT: TypeTag]()
                                                           (implicit executor: ExecutionContext)
  extends AbstractGrpcQuService[Transportable, ObjectT]() {

  val clientId: Context.Key[Key] = Constants.CLIENT_ID_CONTEXT_KEY //plugged by context (server interceptor)

  import java.util.concurrent.Executors

  //scheduler for io-bound (callbacks from other servers)
  val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors + 1)) //or MoreExecutors.newDirectExecutorService

  override def sRequest[T: universe.TypeTag](request: Request[T, ObjectT], responseObserver: StreamObserver[Response[Option[T]]])(implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[ObjectT]], logicalTimestampTransportable: Transportable[ConcreteLogicalTimestamp]): Unit = {
    mapFutureToObserver(agnosticService.sRequest(request, clientId.get()), responseObserver)
  }

  def mapFutureToObserver[T](fut: Future[T], responseObserver: StreamObserver[T]): Unit =
    fut.onComplete {
      case Success(a) =>
        responseObserver.onNext(a)
        responseObserver.onCompleted()
      case Failure(cause) =>
        responseObserver.onError(cause)
        responseObserver.onCompleted()
    }

  override def sObjectRequest(request: ConcreteLogicalTimestamp, responseObserver
  : StreamObserver[ObjectSyncResponse[ObjectT]]): Unit =
    mapFutureToObserver(agnosticService.sObjectRequest(request), responseObserver)

}
