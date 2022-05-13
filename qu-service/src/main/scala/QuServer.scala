//import Implementations.jacksonService

import QuServerBuilder.jacksonSimplePolicyServerBuilder
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ServerBuilder
import qu.protocol.Shared.{QuorumSystemThresholds, ServerInfo}
import qu.protocol.model.ConcreteQuModel._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.reflect.runtime.universe._


//a facade that hides grpc internals
trait QuServer {

  def start(): Unit

  def stop()(implicit executor: ExecutionContext): Future[Unit]
}

//companion object
object QuServer {
  // creation by builder, not factory: def apply()

  //could use builder factory instead of defaultBuilder
  def builder[U](myServerInfo: ServerInfo, thresholds: QuorumSystemThresholds, obj: U): Unit =
    jacksonSimplePolicyServerBuilder[U](myServerInfo, thresholds, obj)
}


class QuServerImpl[Marshallable[_], U](quService: QuServiceImplBase2[Marshallable, U]) extends QuServer {
  private val grpcServer = ServerBuilder.forPort(3).addService(quService).build

  override def start(): Unit = grpcServer.start //grpcServer.start

  override def stop()(implicit executor: ExecutionContext): Future[Unit] = Future {
    //val promise = Promise()
    grpcServer.awaitTermination()
    //promise.future
  }
}

//alternative to apply in companion object
class QuServerBuilder[Marshallable[_], U](private val serviceFactory: (QuorumSystemThresholds, ServerInfo) => QuServiceImpl[Marshallable, U],
                                          private val myServerInfo: ServerInfo,
                                          private val quorumSystemThresholds: QuorumSystemThresholds,
                                          private val obj: U) {

  private val quService: QuServiceImpl[Marshallable, U] = serviceFactory(quorumSystemThresholds, myServerInfo)

  def addOperation[T: TypeTag](implicit
                               marshallableRequest: Marshallable[Request[T, U]],
                               marshallableResponse: Marshallable[Response[Option[T]]],
                               marshallableLogicalTimestamp: Marshallable[LogicalTimestamp],
                               marshallableObjectSyncResponse: Marshallable[ObjectSyncResponse[U]],
                               last: TypeTag[T]):
  QuServerBuilder[Marshallable, U] = {
    quService.addOp[T](last = last,
      marshallableRequest = marshallableRequest,
      marshallableResponse = marshallableResponse,
      marshallableLogicalTimestamp = marshallableLogicalTimestamp,
      marshallableObjectSyncResponse = marshallableObjectSyncResponse)
    this
  }

  def addServer(serverInfo: ServerInfo): QuServerBuilder[Marshallable, U] = {
    quService.addServer(serverInfo)
    this
  }

  def withPort(port: Int): QuServerBuilder[Marshallable, U] = {
    //todo whenre to save this info?
    this
  }

  //todo decide where to put
  def withObj(obj: U): QuServerBuilder[Marshallable, U] = {
    this
  }
  //def addCluster(): Unit = {}

  def build(): QuServer = {
    //validation
    new QuServerImpl(quService)
  } //this or: 1. factory of QUServer, 2. factory of QuServerImpl
}

object QuServerBuilder {
  //hided builder implementations
  //partial application of case classes apply?
  def jacksonSimplePolicyServerBuilder[U](myServerInfo: ServerInfo, thresholds: QuorumSystemThresholds, obj: U) =
    new QuServerBuilder[JavaTypeable, U](???, myServerInfo, thresholds, obj)

}

