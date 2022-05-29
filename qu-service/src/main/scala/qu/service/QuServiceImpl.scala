package qu.service

import io.grpc.Context
import io.grpc.stub.StreamObserver
import qu.MethodDescriptorFactory
import qu.auth.Constants
import qu.model.ConcreteQuModel.hmac
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.storage.ImmutableStorage

import java.security.InvalidParameterException
import java.util.logging.{Level, Logger}
import scala.collection.SortedSet
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}
import scala.math.Ordered.orderingToOrdered
import scala.reflect.runtime.universe._
import scala.util.Success

//import that declares specific dependency
import qu.model.ConcreteQuModel._


class QuServiceImpl[Transportable[_], ObjectT: TypeTag]( //dependencies chosen by programmer
                                                         private val methodDescriptorFactory: MethodDescriptorFactory[Transportable],
                                                         private val policyFactory: (Map[String, Int], QuorumSystemThresholds) => ServerQuorumPolicy[Transportable, ObjectT],
                                                         //dependencies chosen by user
                                                         override val ip: String,
                                                         override val port: Int,
                                                         override val privateKey: String,
                                                         override val obj: ObjectT,
                                                         override val thresholds: QuorumSystemThresholds
                                                       )
  extends AbstractQuService[Transportable, ObjectT](methodDescriptorFactory, policyFactory, thresholds, ip, port, privateKey, obj) {
  private val logger = Logger.getLogger(classOf[QuServiceImpl[Transportable, ObjectT]].getName)
  private var storage = ImmutableStorage[ObjectT]()

  val clientId: Context.Key[Key] = Constants.CLIENT_ID_CONTEXT_KEY //plugged by context (server interceptor)


  import java.util.concurrent.Executors

  //scheduler for io-bound (callbacks from other servers)
  val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors + 1)) //or MoreExecutors.newDirectExecutorService
  //todo scheduler for cpu-bound (computing hmac, for now not used)

  //initialization (todo could have destructured tuple here instead
  var authenticatedReplicaHistory: AuthenticatedReplicaHistory = emptyAuthenticatedRh
  storage = storage.store(emptyLT, (obj, Option.empty)) //must add to store the initial object (passed by param)


  override def sRequest[AnswerT: TypeTag](request: Request[AnswerT, ObjectT],
                                          responseObserver: StreamObserver[Response[Option[AnswerT]]])(implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[ObjectT]],
                                                                                                       logicalTimestampTransportable: Transportable[LogicalTimestamp])
  : Unit = {
    logger.log(Level.INFO, "request received from " + clientId.get, 2) //: " + clientId.get, 2)
    logger.log(Level.INFO, "request with ohs " + request.ohs, 2) //: " + clientId.get, 2)

    val (replicaHistory, _) = authenticatedReplicaHistory
    var answer = Option.empty[AnswerT]

    def replyWith(response: Response[Option[AnswerT]]): Unit = {
      responseObserver.onNext(response)
    }

    //todo not need to pass request if nested def
    def executeOperation(request: Request[AnswerT, ObjectT]): (ObjectT, AnswerT) = {
      //todo can actually happen that a malevolent client can make this exception happen?
      request.operation.getOrElse(throw new RuntimeException("inconsistent protocol state: if classify returns a (inline) method then operation should be defined (not none)")).compute(obj) //for {operation <- request.operation} operation.compute(obj)
    }

    logger.log(Level.INFO, "repairable Thresholds at service side is:" + thresholds.r, 2)


    //(se ohs ha più server/server diversi da me... di me lancio exception) itero i miei server  e per ogni rh (tranne la empty) controllo l'authenticator

    //flatmap(e => (e._1, e._2._1, e._2._2)
    //})
    //.map{ case (id , (rh, hmac2)) => if (hmac(keys(id),rh) != hmac2) (id, emptyAuthenticatedRh)}

    // arh => arh._2 != hmac(keys(arh._1),)).map()

    //val (rh, authenticator) = request.ohs.filterNot(_ == emptyAuthenticatedRh)(server._1)
    //if (authenticator!= hmac(keys(server._1), rh)) request.ohs.
    //todo: to be removed
    def getId(ip: String, port: Int) = ip + port

    def cullRh(ohs: OHS): OHS = ohs. //todo map access like this (to authenticator) could raise exception
      //if there's not the authenticator or if it is invalid the corresponding rh is culled
      map { case (serverId, (rh, authenticator)) => if (!authenticator.contains(getId(ip, port)) || authenticator(getId(ip, port)) != hmac(keys(serverId), rh))
        (serverId, (emptyRh, authenticator)) else (serverId, (rh, authenticator)) //keep authentictor untouched (as in paper)
      }

    //culling invalid Replica histories
    val updatedOhs = cullRh(request.ohs)

    val (opType, (lt, ltCo), ltCurrent) = setup(request.operation, updatedOhs, thresholds.q, thresholds.r, clientId.get())

    //repeated request
    if (contains(replicaHistory, (lt, ltCo))) {
      val (_, answer) = storage.retrieve[AnswerT](lt).getOrElse(throw new Error("inconsistent protocol state: if in replica history must be in store too."))
      replyWith(Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory))
      return //todo put attention if it's possible to express this with a chain of if e.se and only one return
    }

    //validating if ohs current
    if (latestTime(replicaHistory) > ltCurrent) {
      // optimistic query execution
      if (request.operation.getOrElse(false).isInstanceOf[Query[_, _]]) {
        val (obj, _) = storage.retrieve[AnswerT](lt).getOrElse(throw new Error("inconsistent protocol state: if replica history has lt older than ltcur store must contain ltcur too."))
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

    if (opType == ConcreteOperationTypes.METHOD || opType == ConcreteOperationTypes.INLINE_METHOD || opType == ConcreteOperationTypes.COPY) {
      val objAndAnswer = storage.retrieve[AnswerT](lt) //retrieve[T, U](lt)
      if (objAndAnswer.isEmpty && ltCo > emptyLT) {
        /*for { obj <- quorumPolicy.objectSync[T]()} yield*/
        //todo here I require answer as Object since I overwrite it
        quorumPolicy.objectSync(lt).onComplete({
          case Success(obj) => respondWithFoundObjectAndUpdateDataStructures(obj) //here I know that a quorum is found...
          case _ => //what can actually happen here? (malformed json, bad url) those must be notified to server user
        })(ec)
      }
    }

    respondWithFoundObjectAndUpdateDataStructures(obj)

    def respondWithFoundObjectAndUpdateDataStructures(obj: ObjectT): Unit = {
      var answerToReturn = Option.empty[AnswerT]
      if (opType == ConcreteOperationTypes.METHOD || opType == ConcreteOperationTypes.INLINE_METHOD) {
        val (_, answer) = executeOperation(request)
        answerToReturn = Some(answer) //must overwrite
        //if method or inline method operation should not be empty
        if (request.operation.getOrElse(false).isInstanceOf[Query[_, _]]) { //todo ugly
          replyWith(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
          return
        }
      }

      this.synchronized {
        val updatedReplicaHistory: ReplicaHistory = replicaHistory.appended(lt -> ltCo) //with rh as sortedset: replicaHistory + (lt -> ltCo)
        val updatedAuthenticator = updateAuthenticatorFor(keys)(ip)(updatedReplicaHistory)
        authenticatedReplicaHistory = (updatedReplicaHistory, updatedAuthenticator)

        //overriding answer since it's ignored at client side
        if (opType == ConcreteOperationTypes.COPY) {
          answerToReturn = Option.empty
        }
        if (opType == ConcreteOperationTypes.METHOD || opType == ConcreteOperationTypes.INLINE_METHOD || opType == ConcreteOperationTypes.COPY) {
          storage = storage.store[AnswerT](lt, (obj, answerToReturn)) //answer.asInstanceOf[AnswerT])
          //todo: replica history pruning
        }
        logger.log(Level.INFO, "QuServiceImpl is sending...", 2)
        replyWith(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
        responseObserver.onCompleted()
      }
    }
  }


  override def sObjectRequest(request: LogicalTimestamp, //or the wrapping class
                              responseObserver: StreamObserver[ObjectSyncResponse[ObjectT]]): Unit = {
    //devo prevedere il fatto che il server contattato potrebbe non avere questo method descriptor perché lavora su
    //altri oggetti (posso importlo con i generici all'altto della costruzione???)
    responseObserver.onNext(ObjectSyncResponse(StatusCode.SUCCESS,
      storage.retrieveObject(request))) //answer: Option[(ObjectT, AnswerT)])
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

class qu.service.QuServiceImpl[Marshallable[_],U: TypeTag](private val myId: ServerId,
                                                 private val keys: Map[ServerId, String], //this contains mykey too (needed)
                                                 private val thresholds: QuorumSystemThresholds)
  extends QuServiceImplBase2[Marshallable, U] {
 */