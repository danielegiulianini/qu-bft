package qu


import io.grpc.stub.StreamObserver
import Shared.{QuorumSystemThresholds, RecipientInfo}
import qu.protocol.model.ConcreteQuModel._

import scala.reflect.runtime.universe._

//abstract description of QuService functionalities
trait QuService[U] {

  def sRequest[T:TypeTag](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit

  def sObjectRequest[T](request: LogicalTimestamp, responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit
}

//co containing utilities for creation
object QuService {
  //def defaultBuilder[U]() = QuServiceImplBase.QuServiceBuilder.simpleQuorumPolicyJacksonServiceBuilder[U]()
}
