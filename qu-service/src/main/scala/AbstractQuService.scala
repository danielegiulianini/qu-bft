import ServerQuorumPolicy.{ServerQuorumPolicyFactory, simpleJacksonServerQuorumFactory}
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{BindableService, ManagedChannel, ManagedChannelBuilder, MethodDescriptor, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import Shared.{QuorumSystemThresholds, RecipientInfo => ServerInfo}
import qu.protocol.TemporaryConstants._
import qu.protocol.model.ConcreteQuModel
import qu.protocol.{JacksonMethodDescriptorFactory, MethodDescriptorFactory}

import scala.reflect.runtime.universe._
import java.util.Objects

//import that declares specific dependency
import qu.protocol.model.ConcreteQuModel._


/*
//Builder pattern revisited: private setters replacing constructors params (needed
// because I can't instantiate QuService at the end but must do it before, as addOperation must
// work over the same instance of it)
abstract class QuServiceImplBase[Marshallable[_], U] extends BindableService with QuService[U] {
  //(odvrei lasciare solo i getter protected, gli altri provate) se ritorno un implbase nella build allora i private devono essere private , se un QuServiceImpl allora proected

//analogous to protected constructor in classic builder pattern
  protected var obj: U = _ //private  //protected var obj: U = _obj
  protected var serversInfo: Set[ServerInfo] = _ //private  //protected var serversInfo: Set[ServerInfo] = _serversInfo
  protected var quorumPolicy: ServerQuorumPolicy[U] = _ //private  //protected var quorumPolicy: ServerQuorumPolicy[U] = _quorumPolicy
  protected var ssd: ServerServiceDefinition = _ //private

  override def bindService(): ServerServiceDefinition = ssd
}

object QuServiceImplBase {
  //builder used to put construction logic for QuService in one place only
  class QuServiceBuilder[Marshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                             private val policyFactory: Set[ServerInfo] => ServerQuorumPolicy[U]) {

    val quService: QuServiceImpl[Marshallable, U] = null // new QuServiceImpl[U, Marshallable]()
    val ssd = ServerServiceDefinition.builder(SERVICE_NAME)
    var servers = Set[ServerInfo]()

    //todo could go in builder factory
    def withIp(serverIp: String): Unit = {
      //aggiunge se stesso ai server (no, devi aggiungere anche il myId)
    }

    //this precluded me the possibility of using scala name constr as builder
    def addOp[T: TypeTag](implicit
                          marshallableRequest: Marshallable[Request[T, U]],
                          marshallableResponse: Marshallable[Response[Option[T]]],
                          marshallableLogicalTimestamp: Marshallable[LogicalTimestamp],
                          marshallableObjectSyncResponse: Marshallable[ObjectSyncResponse[U]]): QuServiceBuilder[Marshallable, U] = {
      //could be a separate reusable utility to plug into ssd (by implicit conversion)
      def addMethod[X: Marshallable, Y: Marshallable](handler: ServerCalls.UnaryMethod[X, Y]): Unit =
        ssd.addMethod(methodDescriptorFactory.generateMethodDescriptor5[X, Y](METHOD_NAME, SERVICE_NAME),
          ServerCalls.asyncUnaryCall[X, Y](handler))

      println("generating md:")
      addMethod[Request[T, U], Response[Option[T]]]((request, obs) => quService.sRequest(request, obs))
      addMethod[LogicalTimestamp, ObjectSyncResponse[U]]((request, obs) => quService.sObjectRequest(request, obs))
      println("md generated:")
      this
    }

    def addServer(serverInfo: ServerInfo): QuServiceBuilder[Marshallable, U] = {
      Objects.requireNonNull(serverInfo) //require(serverInfo != null)
      servers = servers + serverInfo
      this
    }

    def build(): QuServiceImplBase[Marshallable, U] = {
      //validation
      quService.quorumPolicy = policyFactory(servers)
      //aggiunge se stesso ai server e crea mappa keys
      quService.serversInfo = servers
      quService.ssd = ssd.build()
      quService
    }
  }

  object QuServiceBuilder {

    //hided builder implementations

    def simpleQuorumPolicyJacksonServiceBuilder[U](): QuServiceBuilder[JavaTypeable, U] =
      null //new MyNewServiceBuilder[JavaTypeable](new JacksonMethodDescriptorFactory {}, )
  }
}*/


//QuServiceImplBase without builder... (each method could or not return the QuServiceImplBase itself)
abstract class AbstractQuService[Marshallable[_], U] (
                                                       //dependencies chosen by programmer
                                                       private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                                       private val policyFactory: ServerQuorumPolicyFactory[Marshallable, U],
                                                       //dependencies chosen by user (could go as setter)
                                                       protected val thresholds: QuorumSystemThresholds,
                                                       protected val myServerInfo: ServerInfo,
                                                       protected val obj: U)
  extends BindableService with QuService[U] {
  private val ssd = ServerServiceDefinition.builder(SERVICE_NAME)

  private var servers = Set[ServerInfo]()
  protected val keys = Map[ServerId, String]() //this contains mykey too (needed)
  protected val quorumPolicy: ServerQuorumPolicy[Marshallable, U] = policyFactory(servers, thresholds)

  insertKeyForServer(myServerInfo)

  //this precluded me the possibility of using scala name constr as builder
  def addOp[T]()(implicit
               marshallableRequest: Marshallable[Request[T, U]],
               marshallableResponse: Marshallable[Response[Option[T]]],
               marshallableLogicalTimestamp: Marshallable[LogicalTimestamp],
               marshallableObjectSyncResponse: Marshallable[ObjectSyncResponse[U]],
               last: TypeTag[T]): AbstractQuService[Marshallable, U] = {
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

  def addServer(serverInfo: ServerInfo): AbstractQuService[Marshallable, U] = {
    Objects.requireNonNull(serverInfo) //require(serverInfo != null)
    servers = servers + serverInfo
    insertKeyForServer(serverInfo)
    this
  }

  private def insertKeyForServer(serverInfo: ServerInfo): Unit = keys + serverInfo.ip -> serverInfo.hmacKey

  override def bindService(): ServerServiceDefinition = ssd.build
}

object AbstractQuService {
  type ServiceFactory[Marshallable[_], U] = (QuorumSystemThresholds, ServerInfo, U) => AbstractQuService[Marshallable, U]

  def jacksonSimpleQuorumServiceFactory[U:TypeTag](): ServiceFactory[JavaTypeable, U] = (quorumSystemThresholds, serverInfo, obj) =>
    new QuServiceImpl(new JacksonMethodDescriptorFactory {},
    simpleJacksonServerQuorumFactory(),
    serverInfo, quorumSystemThresholds, obj)
}


















/*
QuServiceImplBase VERSIONS NOT USING BUILDER FOR QUsERVIce
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


/* old with QuService...
class QuServiceImpl[U, Marshallable[_]]( //strategy
                                         private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                         //strategy
                                         private val stubFactory: (String) => GrpcClientStub[Marshallable],
                                         //strategy
                                         private var policyFactory: Map[String, GrpcClientStub[Marshallable]] => ServerQuorumPolicy[U])
  extends MyNewServiceImplBase[Marshallable, U](methodDescriptorFactory, stubFactory, policyFactory) {

  //values to inject
  val keys = Map[String, String]() //this contains mykey too
  val q = 2
  val r = 3
  val clientId = "" //from context (server interceptor)

  //initialization
  var authenticatedReplicaHistory = emptyAuthenticatedRh(keys)

  def sRequest[T](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit = {
    println("received request!")
    val (replicaHistory, authenticator) = authenticatedReplicaHistory
    val answer = Option.empty[T]

    //culling invalid Replica histories
    val updatedOhs = request.
      ohs. //todo map access like this (to authenticator) could raise exception
      map { case (serverId, (rh, authenticator)) => if (authenticator("mioServerId") != hmac(keys(serverId), rh))
        (serverId, (emptyRh, authenticator)) else (serverId, (rh, authenticator))
      } //keep authentictor untouched (as in paper)

    val (opType, (lt, ltCo), ltCurrent) = setup[T, U](request.operation, updatedOhs, q, r, clientId)
    if (contains(replicaHistory, (lt, ltCo))) {
      val (obj, answer) = retrieve[T, U](lt)
      responseObserver.onNext(Response[Option[T]](StatusCode.SUCCESS, Some(answer), 2, (null, Map())))
    }


    this.synchronized {
      //update RH
    }

    responseObserver.onNext(Response[Option[T]](StatusCode.SUCCESS, answer, 2, (null, Map())))
    responseObserver.onCompleted()
  }

  private def sObjectSync(): Unit = {

  }

  def sObjectRequest[T](request: LogicalTimestamp, //oppure
                        responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit = {
    //devo prevedere il fatto che il server potrebbe non avere questo method descriptor perché lavora si
    // altri oggetti (posso importlo con i generici all'altto della costruzione???)
    responseObserver.onNext(ObjectSyncResponse(StatusCode.SUCCESS, null.asInstanceOf[U]))
    responseObserver.onCompleted()
  }

  private def objectSync[T](): Future[(U, T)] = {

    null
  }

  //todo this is not needed here!
  //override protected var stubs: Map[String, GrpcClientStub[Marshallable]] = _
}
*/