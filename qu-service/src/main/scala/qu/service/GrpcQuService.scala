package qu.service

import io.grpc.stub.StreamObserver
import qu.model.QuorumSystemThresholdQuModel._

import scala.reflect.runtime.universe._

trait GrpcQuService[Transportable[_], U] {

  def sRequest[T: TypeTag](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]])
                          (implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[U]],
                           logicalTimestampTransportable: Transportable[LogicalTimestamp]): Unit

  def sObjectRequest(request: LogicalTimestamp, responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit
}