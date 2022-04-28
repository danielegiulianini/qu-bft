import Implementations.jacksonService
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{BindableService, ManagedChannel, ManagedChannelBuilder, MethodDescriptor, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import qu.protocol.CostantiMomentanee._
import qu.protocol.{JacksonMethodDescriptorFactory, MethodDescriptorFactory}

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._

abstract class QuServiceImplBase2[MyMarshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                        private val clientStubFactory: (String) => GrpcClientStub[U, MyMarshallable],
                                                        private var policyFactory: Map[String, GrpcClientStub[U, MyMarshallable]] => ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {
}


abstract class QuServiceImplBase[MyMarshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                       private val clientStubFactory: (String) => GrpcClientStub[U, MyMarshallable],
                                                       private var policyFactory: Map[String, GrpcClientStub[U, MyMarshallable]] => ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, GrpcClientStub[U, MyMarshallable]]
  protected var policy: ServerQuorumPolicy[U] = null

  private val ssd: ServerServiceDefinition.Builder = ServerServiceDefinition.builder(SERVICE_NAME)

  //put here because I must specify dynamically (can't go in builder also because I need to attach it to handlers
  def addOperation[T]()(implicit marshallableT: MyMarshallable[T],
                        marshallableU: MyMarshallable[U],
                        marshallableRequest: MyMarshallable[Request[T, U]],
                        marshallableResponse: MyMarshallable[Response[T, U]],
                        marshallableLogicalTimestamp: MyMarshallable[LogicalTimestamp[T, U]],
                        marshallableObjectSyncResponse: MyMarshallable[ObjectSyncResponse[U]]): Unit = {
    //TODO must register logicalTimeStamp/objectResponse too as this server could send object sync requests
    println("generating md...")
    ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor4[T, U, Request[T, U], Response[T, U]](METHOD_NAME, SERVICE_NAME),
      ServerCalls.asyncUnaryCall[Request[T, U], Response[T, U]]((request: Request[T, U],
                                                                 responseObserver: StreamObserver[Response[T, U]]) => sRequest(request, responseObserver)))
    ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor4[T, U, LogicalTimestamp[T, U], ObjectSyncResponse[U]](METHOD_NAME, SERVICE_NAME),
      ServerCalls.asyncUnaryCall[LogicalTimestamp[T, U], ObjectSyncResponse[U]]((request: LogicalTimestamp[T, U],
                                                                                 responseObserver: StreamObserver[ObjectSyncResponse[U]]) => sObjectRequest(request, responseObserver)))
    println("md generated:")
  }

  //put here together with addOperation for centralizing construction logic
  def addServer(ip: String): Unit = {
    //could return a new, immutable Service! (poi perdo i binding??? già fatti??) no passo avanti il ssd!
    //aggiungo ai server del quorumPolicy un nuovo stub (oppure ricreo come mia quorumPolicy una nuova con un nuovo stub in più...)
    stubs = stubs + (ip -> clientStubFactory(ip))
    policy = policyFactory(stubs)
  }

  override def bindService(): ServerServiceDefinition = ssd.build()
}

//implementation-specific factories
object Implementations {
  //jacksonService factory
  def jacksonService[U]() = new QuServiceImpl[U, JavaTypeable](new JacksonMethodDescriptorFactory {},
    (ip: String) => new JacksonClientStub[U](ManagedChannelBuilder.forAddress(ip, 0).build()),
    (stubs: Map[String, GrpcClientStub[U, JavaTypeable]]) => new SimpleServerQuorumPolicy(stubs)
  )

}

object UserSide {
  //clent can then use jacksonService as a default impl of quService

  val myService = jacksonService[Int]()
  myService.addServer("ciao")
  myService.addOperation[String]()
  myService.addServer("ciao2")
}
