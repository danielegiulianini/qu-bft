import io.grpc.stub.StreamObserver
import qu.protocol.ConcreteQuModel._

//abstract description of QuService functionalities
trait QuService[U] {

  def sRequest[T](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit

  def sObjectRequest[T](request: LogicalTimestamp, responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit
}
