import io.grpc.MethodDescriptor
import io.grpc.stub.StreamObserver
import qu.protocol.{ConcreteQuModel, MethodDescriptorFactory}

import scala.collection.SortedSet
import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._


class QuServiceImpl[U, Marshallable[_]] extends MyNewServiceImplBase[Marshallable, U] {

  //values to inject
  val keys = Map[String, String]() //this contains mykey too
  val q = 2
  val r = 3
  val clientId = "" //from context (server interceptor)

  //scheduler for io-bound (callbacks from other servers)
  //scheduler for cpu-bound (computing hmac)

  //initialization
  var authenticatedReplicaHistory = emptyAuthenticatedRh(keys)

  override def sRequest[T](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit = {
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

  override def sObjectRequest[T](request: LogicalTimestamp, //oppure
                                 responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit = {
    //devo prevedere il fatto che il server potrebbe non avere questo method descriptor perché lavora si
    //altri oggetti (posso importlo con i generici all'altto della costruzione???)
    responseObserver.onNext(ObjectSyncResponse(StatusCode.SUCCESS, null.asInstanceOf[U]))
    responseObserver.onCompleted()
  }

  private def objectSync[T](): Future[(T, U)] = {
    quorumPolicy.objectSync[T]()
  }
}

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