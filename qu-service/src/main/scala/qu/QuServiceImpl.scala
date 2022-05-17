

import com.google.common.util.concurrent.MoreExecutors
import io.grpc.MethodDescriptor
import io.grpc.stub.StreamObserver
import qu.protocol.MethodDescriptorFactory
import Shared.{QuorumSystemThresholds, RecipientInfo}
import qu.protocol.model.{ConcreteQuModel, Storage}

import scala.reflect.runtime.universe._
import java.security.InvalidParameterException
import scala.Option
import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Ordered.orderingToOrdered
import scala.util.Success

//import that declares specific dependency
import qu.protocol.model.ConcreteQuModel._


class QuServiceImpl[Marshallable[_], U: TypeTag]( //dependencies chosen by programmer
                                                  private val methodDescriptorFactory: MethodDescriptorFactory[Marshallable],
                                                  private val policyFactory: (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Marshallable, U],
                                                  //dependencies chosen by user
                                                  override val myServerInfo: RecipientInfo,
                                                  override val thresholds: QuorumSystemThresholds,
                                                  override val obj: U)
  extends AbstractQuService[Marshallable, U](methodDescriptorFactory, policyFactory, thresholds, myServerInfo, obj) {

  private val storage = new StorageWithImmutableMap[U]()

  val clientId = "" //from context (server interceptor)

  //scheduler for io-bound (callbacks from other servers)

  import java.util.concurrent.Executors

  val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors + 1)) //or MoreExecutors.newDirectExecutorService
  //todo scheduler for cpu-bound (computing hmac, for now not used)

  //initialization (todo could have destructured tuple here instead
  var authenticatedReplicaHistory = emptyAuthenticatedRh(keys)
  //must add to store the initial object (passed by param)


  override def sRequest[T: TypeTag](request: Request[T, U], responseObserver: StreamObserver[Response[Option[T]]]): Unit = {
    println("ricevuto richiesta!")

    /*val (replicaHistory, _) = authenticatedReplicaHistory
    var answer = Option.empty[T]

    def replyWith(response: Response[Option[T]]) = {
      responseObserver.onNext(response)
    }

    //todo not need to pass request if nested def
    def executeOperation(request: Request[T, U]): (U, T) = {
      //todo can actually happen that a malevolent client can make this exception happen?
      request.operation.getOrElse(throw new RuntimeException("inconsistent protocol state: if classify returns a (inline) method then operation should be defined (not none)"))(obj) //for {operation <- request.operation} operation.compute(obj)
    }

    println("received request!")

    //culling invalid Replica histories
    val updatedOhs = request.
      ohs. //todo map access like this (to authenticator) could raise exception
      map { case (serverId, (rh, authenticator)) => if (authenticator(myServerInfo.ip) != hmac(keys(serverId), rh))
        (serverId, (emptyRh, authenticator)) else (serverId, (rh, authenticator)) //keep authentictor untouched (as in paper)
      }

    val (opType, (lt, ltCo), ltCurrent) = setup(request.operation, updatedOhs, thresholds.q, thresholds.r, clientId)

    //repeated request
    if (contains(replicaHistory, (lt, ltCo))) {
      val (_, answer) = storage.retrieve[T](lt).getOrElse(throw new Error("inconsistent protocol state: if in replica history must be in store too."))
      replyWith(Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory))
      return //todo put attention if it's possible to express this with a chain of if e.se and only one return
    }

    //validating if ohs current
    if (latestTime(replicaHistory) > ltCurrent) {
      // optimistic query execution
      if (request.operation.isInstanceOf[Option[Query[_, _]]]) {
        val (obj, _) = storage.retrieve[T](lt).getOrElse(throw new Error("inconsistent protocol state: if replica history has lt older than ltcur store must contain ltcur too."))
        val (newObj, opAnswer) = executeOperation(request)
        answer = Some(opAnswer)
        if (newObj != obj) {
          //todo here what to do?
          throw new InvalidParameterException("user sent an update operation as query")
        }
      }
      replyWith(Response(StatusCode.FAIL, answer, authenticatedReplicaHistory))
      return
    }

    if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD || opType == OperationType1.COPY) {
      val objAndAnswer = storage.retrieve[T](lt) //retrieve[T, U](lt)
      if (objAndAnswer.isEmpty && ltCo > emptyLT) {
        /*for { obj <- quorumPolicy.objectSync[T]()} yield*/
        quorumPolicy.objectSync[T]().onComplete({
          case Success(_) => respondWithFoundObjectAndUpdateDataStructures() //here I know that a quorum is found...
          case _ => //what can actually happen here? (malformed json, bad url) those must be notified to server user
        })(ec)
      }
    }

    respondWithFoundObjectAndUpdateDataStructures()

    def respondWithFoundObjectAndUpdateDataStructures(): Unit = {
      if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD) {
        val (_, answer) = executeOperation(request)
        if (request.operation.isInstanceOf[Query[_, _]]) {
          replyWith(Response(StatusCode.SUCCESS, Some(answer), authenticatedReplicaHistory))
          return
        }
      }

      this.synchronized {
        val updatedReplicaHistory: ReplicaHistory = replicaHistory + ((lt, ltCo))
        val updatedAuthenticator = updateAuthenticatorFor(keys)(myServerInfo.ip)(updatedReplicaHistory)
        authenticatedReplicaHistory = (updatedReplicaHistory, updatedAuthenticator)
        if (opType == OperationType1.METHOD || opType == OperationType1.INLINE_METHOD || opType == OperationType1.COPY) {
          storage.store[T](lt, (obj, answer))
        }
        //todo: replica history pruning
      }
      replyWith(Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory))
    }*/
  }


  override def sObjectRequest[T](request: LogicalTimestamp, //oppure
                                 responseObserver: StreamObserver[ObjectSyncResponse[U]]): Unit = {
    //devo prevedere il fatto che il server potrebbe non avere questo method descriptor perché lavora si
    //altri oggetti (posso importlo con i generici all'altto della costruzione???)
    responseObserver.onNext(ObjectSyncResponse(StatusCode.SUCCESS, null.asInstanceOf[U]))
    responseObserver.onCompleted()
  }
}





/*with pattern matching instad of if -else:
request.operation match {
    case _: Query[_, _] =>
      val answer = for {
        ab <- retrieve[T, U](latestTime(authenticatedReplicaHistory._1))
        op <- request.operation
      } yield op.compute(ab._2)
  }*/

/*QuService' constructor before:

class QuServiceImpl[Marshallable[_],U: TypeTag](private val myId: ServerId,
                                                 private val keys: Map[ServerId, String], //this contains mykey too (needed)
                                                 private val thresholds: QuorumSystemThresholds)
  extends QuServiceImplBase2[Marshallable, U] {
 */