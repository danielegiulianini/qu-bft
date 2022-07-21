package qu.service

import io.grpc.stub.StreamObserver
import qu.model.ConcreteQuModel._

import scala.reflect.runtime.universe._

//abstract description of QuService functionalities
trait GrpcQuService[Transportable[_], U] {

  def sRequest[T: TypeTag](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]])(implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[U]], logicalTimestampTransportable: Transportable[LogicalTimestamp]): Unit

  def sObjectRequest(request: LogicalTimestamp, responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit
}