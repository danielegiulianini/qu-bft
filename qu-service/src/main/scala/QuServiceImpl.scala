import io.grpc.MethodDescriptor
import io.grpc.stub.StreamObserver
import qu.protocol.{ConcreteQuModel, MethodDescriptorFactory}

import scala.Option
import scala.collection.SortedSet
import scala.concurrent.Future
import scala.math.Ordered.orderingToOrdered
import scala.reflect.internal.Flags.METHOD
import scala.util.Success

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._


class QuServiceImpl[U, Marshallable[_]] extends MyNewServiceImplBase[Marshallable, U] {

  //values to inject
  val keys = Map[String, String]() //this contains mykey too (needed)
  val q = 2
  val r = 3
  val clientId = "" //from context (server interceptor)
  val myId: ServerId = "myIp"

  //scheduler for io-bound (callbacks from other servers)
  //scheduler for cpu-bound (computing hmac)

  //initialization (todo could have destructured tuple here instead
  var authenticatedReplicaHistory = emptyAuthenticatedRh(keys)

  override def sRequest[T](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit = {
    def replyWith(response: Response[Option[T]]) = {
      responseObserver.onNext(response)
    }

    println("received request!")

    val (replicaHistory, authenticator) = authenticatedReplicaHistory

    val answer = Option.empty[T]

    //culling invalid Replica histories
    val updatedOhs = request.
      ohs. //todo map access like this (to authenticator) could raise exception
      map { case (serverId, (rh, authenticator)) => if (authenticator("mioServerId") != hmac(keys(serverId), rh))
        (serverId, (emptyRh, authenticator)) else (serverId, (rh, authenticator))//keep authentictor untouched (as in paper)
      }

    val (opType, (lt, ltCo), ltCurrent) = setup(request.operation, updatedOhs, q, r, clientId)

    //repeated request
    if (contains(replicaHistory, (lt, ltCo))) {
      val objAnswer = retrieve[T, U](lt)
      replyWith(Response(StatusCode.SUCCESS, objAnswer.map(_._1), authenticatedReplicaHistory))
      return //todo put attention if it's possible to express this with a chain of if e.se and only one return
    }

    if (latestTime(authenticatedReplicaHistory._1) > ltCurrent) {
      // optimistic query execution
      if (request.operation.isInstanceOf[Query[_, _]]) {
        //Option.empty.getOrElse(throw new InconsistentProtocolState("Can't."))
        /*request.operation match {
            case _: Query[_, _] =>
              val answer = for {
                ab <- retrieve[T, U](latestTime(authenticatedReplicaHistory._1))
                op <- request.operation
              } yield op.compute(ab._2)
          }*/
      }
      replyWith(Response(StatusCode.FAIL, answer, authenticatedReplicaHistory))
      return
    }

    if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD || opType == OperationType1.COPY) {
      //retrieve,,,
      if (true) {
        /*for {
          obj <- quorumPolicy.objectSync[T]()
        } yield*/

        import scala.concurrent.ExecutionContext.Implicits.global
        quorumPolicy.objectSync[T]().onComplete({
          //here I know that a quorum is found
          case Success(response) => sharedCode()
          case _ => //this can be removed by pecifying other...)
        })
      }
    }

    sharedCode()

    //todo not need to pass request if nested def
    def executeOperation[T](request: Request[T, U]): Unit = {
      request.operation.getOrElse(throw new RuntimeException("inconsistent protocol state")).compute(obj)
      for {operation <- request.operation} operation.compute(obj)
    }

    def sharedCode(): Unit = {
      if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD) {
        executeOperation(request)
        if (request.operation.isInstanceOf[Query[_, _]]) {
          replyWith(Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory))
          return
        }
      }

      this.synchronized {
        //update ReplicaHistory
        //authenticatedReplicaHistory._1 + (lt, ltCo)
        //update authenticator
        updateAuthenticatorFor(keys)(myId)(replicaHistory)

        if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD || opType == OperationType1.COPY) {
          store(lt, (obj, answer))
        }

        //replica history pruning
        //if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD) {
        //}
      }
      replyWith(Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory))
    }
  }


  override def sObjectRequest[T](request: LogicalTimestamp, //oppure
                                 responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit = {
    //devo prevedere il fatto che il server potrebbe non avere questo method descriptor perché lavora si
    //altri oggetti (posso importlo con i generici all'altto della costruzione???)
    responseObserver.onNext(ObjectSyncResponse(StatusCode.SUCCESS, null.asInstanceOf[U]))
    responseObserver.onCompleted()
  }
}
