package qu


import io.grpc.stub.StreamObserver
import qu.protocol.model.ConcreteQuModel._

import scala.reflect.runtime.universe._

//abstract description of QuService functionalities
trait QuService[U] {

  def sRequest[T:TypeTag](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit

  def sObjectRequest[T:TypeTag](request: LogicalTimestamp, responseObserver: StreamObserver[ObjectSyncResponse[U, T]]): Unit
}

//co containing utilities for creation
object QuService {
  //def defaultBuilder[U]() = QuServiceImplBase.QuServiceBuilder.simpleQuorumPolicyJacksonServiceBuilder[U]()
}
