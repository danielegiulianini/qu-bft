import io.grpc.{BindableService, MethodDescriptor, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import qu.protocol.CostantiMomentanee._
import qu.protocol.MethodDescriptorFactory

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._

//poi spstare tutti metodi di là
//poi si compone di quorumPolicy, non di clientStub
// abstract class QuServiceImplBase[MyMarshallable[_], U](mds: Set[MethodDescriptor[Request[_, U], Response[_, U]]])
abstract class QuServiceImplBase[MyMarshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                       private val clientStubFactory: (String) => GrpcClientStub[U, MyMarshallable])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, GrpcClientStub[U, MyMarshallable]] = Map()

  private val ssd: ServerServiceDefinition.Builder = ServerServiceDefinition.builder(SERVICE_NAME)

  //put here because I must specify dynamically (can't go in builder also because I need to attach it to handlers
  def addOperation[T]()(implicit enc: MyMarshallable[T],
                        dec: MyMarshallable[U],
                        marshallable: MyMarshallable[Request[T, U]],
                        marshallable3: MyMarshallable[Response[T, U]]): Unit = {
    //TODO must register logicalTimeStamp/objectResponse too as this server could send object sync requests
    println("generating md...")
    ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor[T, U](METHOD_NAME, SERVICE_NAME),
      ServerCalls.asyncUnaryCall[Request[T, U], Response[T, U]]((request: Request[T, U],
                                                                 responseObserver: StreamObserver[Response[T, U]]) => sResponse(request, responseObserver)))
    println("md generated:")
  }

  //put here for centralizing construction logic wrt addOperation
  def addServer(ip: String) : Unit = {
    //TODO must register logicalTimeStamp/objectResponse too as this server could send object sync requests
    //could return a new, immutable Service!
    stubs = stubs + (ip -> clientStubFactory(ip))
  }

  override def bindService(): ServerServiceDefinition = ssd.build()
}
