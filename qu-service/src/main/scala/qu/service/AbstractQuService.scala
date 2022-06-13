package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.stub.ServerCalls
import io.grpc.{BindableService, MethodDescriptor, ServerCallHandler, ServerMethodDefinition, ServerServiceDefinition}
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.ServerInfo
import qu.service.quorum.ServerQuorumPolicy.{ServerQuorumPolicyFactory, simpleDistributedJacksonServerQuorumFactory}
import qu.{CachingMethodDescriptorFactory, JacksonMethodDescriptorFactory, MethodDescriptorFactory, Shutdownable}
import qu.stub.client.RecipientInfo._
import qu.service.quorum.ServerQuorumPolicy
import qu.stub.client.{AbstractRecipientInfo, RecipientInfo}

import java.util.Objects
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.runtime.universe._


//import that declares specific dependency
import qu.model.ConcreteQuModel._


class CachingServiceServerDefinitionBuilder(private var serviceName: String) {
  private val builder = ServerServiceDefinition.builder(serviceName)
  private var mds = Set[String]()

  def addMethod[ReqT, RespT](`def`: MethodDescriptor[ReqT, RespT], handler: ServerCallHandler[ReqT, RespT]): CachingServiceServerDefinitionBuilder = {
    if (!mds.contains(`def`.getFullMethodName)) {
      mds = mds + `def`.getFullMethodName
      builder.addMethod(`def`, handler)
    }
    this
  }

  def build(): ServerServiceDefinition = builder.build
}

object CachingServiceServerDefinitionBuilder {
  def apply(serviceName: String) =
    new CachingServiceServerDefinitionBuilder(serviceName)
}

//QuServiceImplBase without builder... (each method could or not return the QuServiceImplBase itself)
abstract class AbstractQuService[Transportable[_], ObjectT]( //dependencies chosen by programmer
                                                             private val methodDescriptorFactory: MethodDescriptorFactory[Transportable],
                                                             private val policyFactory: ServerQuorumPolicyFactory[Transportable, ObjectT],
                                                             //dependencies chosen by user (could go as setter)
                                                             protected val thresholds: QuorumSystemThresholds,
                                                             protected val ip: String,
                                                             protected val port: Int,
                                                             protected val privateKey: String,
                                                             protected val obj: ObjectT)(implicit executor: ExecutionContext)
  extends BindableService with QuService[Transportable, ObjectT] with Shutdownable {
  private val ssd = CachingServiceServerDefinitionBuilder(SERVICE_NAME)

  protected var servers: Set[RecipientInfo] = Set[RecipientInfo]()
  protected var keysSharedWithMe: Map[ServerId, Key] = Map[ServerId, String]() //this contains mykey too (needed)
  protected var quorumPolicy: ServerQuorumPolicy[Transportable, ObjectT] = policyFactory(servers, thresholds)

  insertKeyForServer(id(RecipientInfo(ip, port)), privateKey)

  //this precluded me the possibility of using scala name constr as builder
  def addOperationOutput[OperationOutputT]()(implicit
                                             transportableRequest: Transportable[Request[OperationOutputT, ObjectT]],
                                             transportableResponse: Transportable[Response[Option[OperationOutputT]]],
                                             transportableLogicalTimestamp: Transportable[LogicalTimestamp],
                                             transportableObjectSyncResponse: Transportable[ObjectSyncResponse[ObjectT]],
                                             transportableObjectRequest: Transportable[Request[Object, ObjectT]],
                                             transportableObjectResponse: Transportable[Response[Option[Object]]],
                                             last: TypeTag[OperationOutputT]): AbstractQuService[Transportable, ObjectT] = {
    def addMethod[X: Transportable, Y: Transportable](handler: ServerCalls.UnaryMethod[X, Y]): Unit =
      ssd.addMethod(
        methodDescriptorFactory.generateMethodDescriptor[X, Y](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME),
        ServerCalls.asyncUnaryCall[X, Y](handler))


    addMethod[Request[OperationOutputT, ObjectT], Response[Option[OperationOutputT]]]((request, obs) => sRequest(request, obs))
    addMethod[LogicalTimestamp, ObjectSyncResponse[ObjectT]]((request, obs) => sObjectRequest(request, obs))
    //adding mds needed for handling barrier and copy requests
    addMethod[Request[Object, ObjectT], Response[Option[Object]]]((request, obs) => sRequest(request, obs))
    this
  }

  def addServer(ip: String, port: Int, keySharedWithMe: String): AbstractQuService[Transportable, ObjectT] = {
    servers = servers + RecipientInfo(ip, port)
    insertKeyForServer(id(ServerInfo(ip, port, keySharedWithMe)), keySharedWithMe)
    quorumPolicy = policyFactory(servers, thresholds)
    this
  }

  def addServer(serverInfo: ServerInfo): AbstractQuService[Transportable, ObjectT] = {
    Objects.requireNonNull(serverInfo) //require(serverInfo != null)
    insertKeyForServer(id(serverInfo), serverInfo.keySharedWithMe)
    addServer(serverInfo.ip, serverInfo.port, serverInfo.keySharedWithMe)
  }

  private def insertKeyForServer(id: ServerId, keySharedWithMe: Key): Unit = {
    keysSharedWithMe = keysSharedWithMe + (id -> keySharedWithMe)
  }


  override def bindService(): ServerServiceDefinition = ssd.build()

  override def shutdown(): Future[Unit] = quorumPolicy.shutdown

  override def isShutdown: Flag = quorumPolicy.isShutdown
}


object AbstractQuService {

  //for reducing number of parameters
  case class ServerInfo(ip: String, port: Int, keySharedWithMe: String) extends AbstractRecipientInfo

  type ServiceFactory[Transportable[_], U] =
    (ServerInfo, U, QuorumSystemThresholds) => AbstractQuService[Transportable, U]

  def jacksonSimpleQuorumServiceFactory[U: TypeTag]()(implicit executor: ExecutionContext): ServiceFactory[JavaTypeable, U] = (serverInfo, obj, quorumSystemThresholds) =>
    new QuServiceImpl(
      methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable],
      policyFactory = simpleDistributedJacksonServerQuorumFactory(),
      ip = serverInfo.ip,
      port = serverInfo.port,
      privateKey = serverInfo.keySharedWithMe,
      obj = obj,
      thresholds = quorumSystemThresholds)


  //todo (if wanting to use named params for more readability)
  def jacksonSimpleQuorumServiceFactoryWithServerInfo[U: TypeTag]()(serverInfo: ServerInfo,
                                                                    obj: U,
                                                                    quorumSystemThresholds: QuorumSystemThresholds)(implicit executor: ExecutionContext) =
    new QuServiceImpl(
      methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable] {},
      policyFactory = simpleDistributedJacksonServerQuorumFactory[U](),
      ip = serverInfo.ip,
      port = serverInfo.port,
      privateKey = serverInfo.keySharedWithMe,
      obj = obj,
      thresholds = quorumSystemThresholds)
}

/* old:
object AbstractQuService {

  //factory replaced by actual service if using functional builder
  type ServiceFactory[Marshallable[_], U] = (QuorumSystemThresholds, ServerInfo, U) => AbstractQuService[Marshallable, U]

  def jacksonSimpleQuorumServiceFactory[U:TypeTag](): ServiceFactory[JavaTypeable, U] = (quorumSystemThresholds, serverInfo, obj) =>
    new QuServiceImpl(new JacksonMethodDescriptorFactory {},
    simpleJacksonServerQuorumFactory(),
    serverInfo, quorumSystemThresholds, obj)
}

 */


/*
abandoned because of many protected fields (uninitialized at the beginning, but actually this isn't a problem!)
//interesting Builder pattern revisited: private setters replacing constructors params (needed
// because I can't instantiate QuService at the end but must do it before, as addOperation must
// work over the same instance of it)
abstract class QuServiceImplBase[Marshallable[_], U] extends BindableService with QuService[U] {
  //(odvrei lasciare solo i getter protected, gli altri provate) se ritorno un implbase nella build allora i private devono essere private , se un qu.service.QuServiceImpl allora proected

//set of protected methods (analogous to protected constructor in "classic" builder pattern)
  protected var obj: U = _ //private  //protected var obj: U = _obj
  protected var serversInfo: Set[ServerInfo] = _ //private  //protected var serversInfo: Set[ServerInfo] = _serversInfo
  protected var quorumPolicy: qu.service.quorum.ServerQuorumPolicy[U] = _ //private  //protected var quorumPolicy: qu.service.quorum.ServerQuorumPolicy[U] = _quorumPolicy
  protected var ssd: ServerServiceDefinition = _ //private

  override def bindService(): ServerServiceDefinition = ssd
}

object QuServiceImplBase {
  //builder used to put construction logic for QuService in one place only
  class QuServiceBuilder[Marshallable[_], U](private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                             private val policyFactory: Set[ServerInfo] => qu.service.quorum.ServerQuorumPolicy[U]) {

    val quService: qu.service.QuServiceImpl[Marshallable, U] = null // new qu.service.QuServiceImpl[U, Marshallable]()
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


/*
QuServiceImplBase VERSIONS NOT USING BUILDER FOR QUsERVIce
//QuServiceImplBase2 reducing dependencies from  4 to 3
abstract class QuServiceImplBase2[MyMarshallable[_], U](private var obj: U,
                                                        private val methodDescriptorFactory: MethodDescriptorFactory[MyMarshallable],
                                                        private var policyFactory: Map[String, String] => qu.service.quorum.ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, qu.GrpcClientStub[MyMarshallable]]
  protected var policy: qu.service.quorum.ServerQuorumPolicy[U] = null

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
                                                       private val clientStubFactory: (String) => qu.GrpcClientStub[MyMarshallable],
                                                       private var policyFactory: Map[String, qu.GrpcClientStub[MyMarshallable]] => qu.service.quorum.ServerQuorumPolicy[U])
  extends BindableService with QuService[U] {

  protected var stubs: Map[String, qu.GrpcClientStub[MyMarshallable]]
  protected var policy: qu.service.quorum.ServerQuorumPolicy[U] = null

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
  def jacksonService[U]() = new qu.service.QuServiceImpl[U, JavaTypeable](new JacksonMethodDescriptorFactory {},
    (ip: String) => new JacksonClientStub(ManagedChannelBuilder.forAddress(ip, 0).build()),
    (stubs: Map[String, qu.GrpcClientStub[JavaTypeable]]) => new qu.service.quorum.SimpleServerQuorumPolicy(stubs)
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
class qu.service.QuServiceImpl[U, Marshallable[_]]( //strategy
                                         private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                         //strategy
                                         private val stubFactory: (String) => qu.GrpcClientStub[Marshallable],
                                         //strategy
                                         private var policyFactory: Map[String, qu.GrpcClientStub[Marshallable]] => qu.service.quorum.ServerQuorumPolicy[U])
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
  //override protected var stubs: Map[String, qu.GrpcClientStub[Marshallable]] = _
}
*/