import GrpcClientStub.JacksonClientStub
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{BindableService, ManagedChannel, ManagedChannelBuilder, MethodDescriptor, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import qu.protocol.TemporaryConstants._
import qu.protocol.{ConcreteQuModel, JacksonMethodDescriptorFactory, MethodDescriptorFactory}

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._


class MyNewServiceImplBase[Marshallable[_], U] protected extends BindableService with QuService[U] {
  //Builder pattern revisited: private setters replacing constructors params (needed
  // because I can't instantiate QuService at the end but must do it before, as addOperation must
  // work over the same instance of it)

  //equivalent, but with scala style
  protected var obj: U = _obj
  private var _obj: U = _

  protected var serversInfo: Set[ServerInfo] = _serversInfo
  private var _serversInfo: Set[ServerInfo] = _

  protected var quorumPolicy: ServerQuorumPolicy[U] = _quorumPolicy
  private var _quorumPolicy: ServerQuorumPolicy[U] = _

  private var ssd: ServerServiceDefinition = _

  override def bindService(): ServerServiceDefinition = ssd


  //should be protected
  override def sRequest[T](request: ConcreteQuModel.Request[T, U],
                           responseObserver: StreamObserver[ConcreteQuModel.Response[Option[T]]]): Unit = ???

  override def sObjectRequest[T](request: ConcreteQuModel.LogicalTimestamp,
                                 responseObserver: StreamObserver[ConcreteQuModel.ObjectSyncResponse[U]]): Unit = ???

  //builder used to put construction logic for QuService in one place only
  class MyNewServiceBuilder[Marshallable[_]](private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                             private val policyFactory: Set[ServerInfo] => ServerQuorumPolicy[U]) {

    val quService = new MyNewServiceImplBase[Marshallable, U]()
    val ssd = ServerServiceDefinition.builder(SERVICE_NAME)
    var servers = Set[ServerInfo]()

    //this precluded me the possibility of using scala name constr as builder
    def addOp[T](implicit
                 marshallableRequest: Marshallable[Request[T, U]],
                 marshallableResponse: Marshallable[Response[Option[T]]],
                 marshallableLogicalTimestamp: Marshallable[LogicalTimestamp],
                 marshallableObjectSyncResponse: Marshallable[ObjectSyncResponse[U]]) = {
      //could be a separate reusable utility to plug into ssd (by implicit conversion)
      def addMethod[X: Marshallable, Y: Marshallable](handler: ServerCalls.UnaryMethod[X, Y]): Unit =
        ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor5[X, Y](METHOD_NAME, SERVICE_NAME),
          ServerCalls.asyncUnaryCall[X, Y](handler))

      println("generating md:")
      addMethod[Request[T, U], Response[Option[T]]]((request, obs) => sRequest(request, obs))
      addMethod[LogicalTimestamp, ObjectSyncResponse[U]]((request, obs) => sObjectRequest(request, obs))
      println("md generated:")
      this
    }

    def addServer(serverInfo: ServerInfo) = {
      require(serverInfo != null)
      servers = servers + serverInfo
      this
    }

    def build(): MyNewServiceImplBase[Marshallable, U] = {
      quService._quorumPolicy = policyFactory(servers)
      //aggiunge se stesso ai server e crea mappa keys
      quService._serversInfo = servers
      quService.ssd = ssd.build()
      quService
    }
  }


  object MyNewServiceBuilder {

    //la factory del builder... che sceglie una delle implementazioni (hided)

    //def apply[U](): MyNewServiceBuilder[JavaTypeable] =

    object Implementations {
      def simpleQuorumPolicyJacksonServiceBuilder[U]() =
        null//new MyNewServiceBuilder[JavaTypeable](new JacksonMethodDescriptorFactory {}, )

      //def simpleQuorumPolicyPlayJsonServiceBuilder() = null
    }
  }
}

object UserSide {

}

case class ServerInfo(ip: String, hmacKey: String, tlsPrivateKey: String, tlsPublicKey: String)

case class QuorumSystemThresholds(r: Int, q: Int)

















/*
//QuServiceImplBase2 reducing dependencies from  4 to 3
abstract class QuServiceImplBase2[MyMarshallable[_], U](private var obj: U,
                                                        private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                        private var policyFactory: Map[String, String] => ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, GrpcClientStub[MyMarshallable]]
  protected var policy: ServerQuorumPolicy[U] = null

  //put here together with addOperation for centralizing construction logic (in costructor should be better?)
  def addServer(ip: String): Unit = {
    //could return a new, immutable Service! (poi perdo i binding??? già fatti??) no passo avanti il ssd!
    //aggiungo ai server del quorumPolicy un nuovo stub (oppure ricreo come mia quorumPolicy una nuova con un nuovo stub in più...)
    stubs = stubs + (ip -> clientStubFactory(ip))
    policy = policyFactory(stubs)
  }
}

//QuServiceImplBase2 using one exceeding dependencies
abstract class QuServiceImplBase[MyMarshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                       private val clientStubFactory: (String) => GrpcClientStub[MyMarshallable],
                                                       private var policyFactory: Map[String, GrpcClientStub[MyMarshallable]] => ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, GrpcClientStub[MyMarshallable]]
  protected var policy: ServerQuorumPolicy[U] = null

  private val ssd: ServerServiceDefinition.Builder = ServerServiceDefinition.builder(SERVICE_NAME)

  //put here because I must specify dynamically (can't go in builder also because I need to attach it to handlers
  def addOperation[T]()(implicit
                        marshallableRequest: MyMarshallable[Request[T, U]],
                        marshallableResponse: MyMarshallable[Response[Option[T]]],
                        marshallableLogicalTimestamp: MyMarshallable[LogicalTimestamp],
                        marshallableObjectSyncResponse: MyMarshallable[ObjectSyncResponse[U]]): Unit = {
    println("generating md...")
    ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor5[Request[T, U], Response[Option[T]]](METHOD_NAME, SERVICE_NAME),
      ServerCalls.asyncUnaryCall[Request[T, U], Response[Option[T]]]((request: Request[T, U],
                                                                      responseObserver: StreamObserver[Response[Option[T]]]) => sRequest(request, responseObserver)))
    ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor5[LogicalTimestamp, ObjectSyncResponse[U]](METHOD_NAME, SERVICE_NAME),
      ServerCalls.asyncUnaryCall[LogicalTimestamp, ObjectSyncResponse[U]]((request: LogicalTimestamp,
                                                                           responseObserver: StreamObserver[ObjectSyncResponse[U]]) => sObjectRequest(request, responseObserver)))
    println("md generated:")
  }

  //put here together with addOperation for centralizing construction logic (in costructor should be better?)
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
    (ip: String) => new JacksonClientStub(ManagedChannelBuilder.forAddress(ip, 0).build()),
    (stubs: Map[String, GrpcClientStub[JavaTypeable]]) => new SimpleServerQuorumPolicy(stubs)
  )
}

object UserSide {
  //client can then use jacksonService as a default impl of quService

  val myService = jacksonService[Int]()
  myService.addServer("ciao")
  myService.addOperation[String]()
  myService.addServer("ciao2")
}*/
