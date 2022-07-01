package qu.service

import io.grpc.{BindableService, Context, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import presentation.MethodDescriptorFactory
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.RecipientInfo.id
import qu.auth.common.Constants
import qu.{AbstractRecipientInfo, RecipientInfo, Shutdownable}
import qu.model.ConcreteQuModel.hmac
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.service.AbstractQuService.ServerInfo
import qu.service.quorum.ServerQuorumPolicy
import qu.service.quorum.ServerQuorumPolicy.ServerQuorumPolicyFactory
import qu.storage.{ImmutableStorage, Storage}

import java.util.Objects
import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import scala.math.Ordered.orderingToOrdered
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

//import that declares specific dependency
import qu.model.ConcreteQuModel._


class QuServiceImpl[Transportable[_], ObjectT: TypeTag](override val ip: String,
                                                        override val port: Int,
                                                        override val privateKey: String,
                                                        override val obj: ObjectT,
                                                        override val thresholds: QuorumSystemThresholds,
                                                        private var storage: Storage[ObjectT])
                                                       (implicit executor: ExecutionContext)
  extends AbstractQuService[Transportable, ObjectT](thresholds, ip, port, privateKey, obj) {

  import qu.LoggingUtils._

  private val logger = Logger.getLogger(classOf[QuServiceImpl[Transportable, ObjectT]].getName)
  implicit val Prefix = PrefixImpl(id(RecipientInfo(ip, port)))

  //must be protected from concurrent access
  storage = storage.store[ObjectT](emptyLT, (obj, Option.empty)) //must add to store the initial object (passed by param)

  val clientId: Context.Key[Key] = Constants.CLIENT_ID_CONTEXT_KEY //plugged by context (server interceptor)

  import java.util.concurrent.Executors

  //scheduler for io-bound (callbacks from other servers)
  val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors + 1)) //or MoreExecutors.newDirectExecutorService
  //scheduler for cpu-bound (computing hmac, for now not used)?

  var authenticatedReplicaHistory: AuthenticatedReplicaHistory = emptyAuthenticatedRh


  override def sRequest[AnswerT: TypeTag](request: Request[AnswerT, ObjectT],
                                          responseObserver: StreamObserver[Response[Option[AnswerT]]])(implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[ObjectT]],
                                                                                                       logicalTimestampTransportable: Transportable[LogicalTimestamp])
  : Unit = {


    def replyWithResponse(response: Response[Option[AnswerT]]): Unit = {
      replyWith(response, responseObserver)
    }

    //todo not need to pass request if nested def
    def executeOperation(request: Request[AnswerT, ObjectT], obj: ObjectT): (ObjectT, AnswerT) = {
      //todo can actually happen that a malevolent client can make this exception
      // happen and break the server? no, it's not breaking it!
      request.operation.getOrElse(throw new RuntimeException("inconsistent protocol state: if " +
        "classify returns a (inline) method operation should be defined (not none)")
      ).compute(obj) //for {operation <- request.operation} operation.compute(obj)
    }

    val (myReplicaHistory, _) = authenticatedReplicaHistory
    var answerToReturn = Option.empty[AnswerT]
    var objToWorkOn = obj

    def cullRh(ohs: OHS): OHS = {
      ohs. //todo map access like this (to authenticator) could raise exception
        //if there's not the authenticator or if it is invalid the corresponding rh is culled
        map { case (serverId, (rh, authenticator)) =>



          if ((!authenticator.contains(id(RecipientInfo(ip, port))) ||
            authenticator(id(RecipientInfo(ip, port))) != hmac(keysSharedWithMe(serverId), rh)) && rh != emptyRh) { //mut use rh here! (not replicahistory)
            println("CULLLLLEDDDD, reason: not contained? " + (!authenticator.contains(id(RecipientInfo(ip, port)))) + "(rh is: " + rh + ").")
            if (authenticator.contains(id(RecipientInfo(ip, port))))
              println("or auth differs? " + (authenticator(id(RecipientInfo(ip, port))) != hmac(keysSharedWithMe(serverId), rh)))

            (serverId, (emptyRh, authenticator))
          }
          else (serverId, (rh, authenticator)) //keep authentictor untouched (as in paper)
        }
    }

    //culling invalid Replica histories
    val updatedOhs = cullRh(request.ohs)

    logger.logWithPrefix(Level.INFO, "SEEEEEEEEERVERRRRRRRRRRR la ohs updated is: " + updatedOhs)
    val (opType, (lt, ltCo), ltCurrent) = setup(request.operation, updatedOhs, thresholds.q, thresholds.r, clientId.get())
    logger.logWithPrefix(msg = "(lt, ltCo) got by request " + request + " is: " + (lt, ltCo))

    logger.logWithPrefix(Level.INFO, "SEEEEEEEEERVERRRRRRRRRRR ltCo ritornato da setup is: " + ltCo)

    //repeated request
    if (contains(myReplicaHistory, (lt, ltCo))) {

      logger.logWithPrefix(msg = "repeated request detected! from request: " + request + " (opType:  " + opType + "), operation: " + request.operation)

      val answer = if (opType != ConcreteOperationTypes.BARRIER && opType != ConcreteOperationTypes.INLINE_BARRIER) {
        val (_, answer) = storage.retrieve[AnswerT](lt).getOrElse(throw new Error("inconsistent protocol state: if in replica history must be in store too."))
        answer
      } else None

      val response = Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory)
      replyWithResponse(response)

      logger.log(Level.INFO, "repeated request detected! sending response" + response)
      return
    }

    //validating if ohs current
    if (latestTime(myReplicaHistory) > ltCurrent) {
      logger.logWithPrefix(Level.INFO, "stall ohs detected!")

      // optimistic query execution
      if (request.operation.getOrElse(false).isInstanceOf[Query[_, _]]) {
        logger.logWithPrefix(Level.INFO, "Since query is required, optimistic query execution, retrieving obj with lt " + lt)
        val obj = storage.retrieveObject(latestTime(myReplicaHistory))
          .getOrElse(throw new Error("inconsistent protocol state: if replica history has lt " +
            "older than ltCur store must contain ltCur too."))
        val (newObj, opAnswer) = executeOperation(request, obj)
        objToWorkOn = newObj
        answerToReturn = Some(opAnswer)
        if (newObj != obj) {
          //no need to check for a update passed as query (since checked at the first time)
        }
      }

      replyWithResponse(Response(StatusCode.FAIL, answerToReturn, authenticatedReplicaHistory))
      return
    }

    //Retrieve conditioned-on object so that method can be invoked
    if (opType == ConcreteOperationTypes.METHOD
      || opType == ConcreteOperationTypes.INLINE_METHOD
      || opType == ConcreteOperationTypes.COPY) {
      val retrievedObj = storage.retrieveObject(ltCo)

      if (retrievedObj.isEmpty && ltCo > emptyLT) {
        logger.logWithPrefix(Level.INFO, "object version NOT available, object-syncing for lt " + ltCo)
        quorumPolicy.objectSync(ltCo).onComplete({
          case Success(obj) => onObjectRetrieved(obj) //here I know that a quorum is found...
          case Failure(thr) => throw thr //what can actually happen here? (malformed json, bad url) those must be notified to server user!
        })
      } else {
        objToWorkOn = retrievedObj.getOrElse(throw new Exception("just checked if it was not none!"))
        logger.logWithPrefix(Level.INFO, "object with lt " + ltCo + "(" + obj + ") available here")
      }
    }

    onObjectRetrieved(objToWorkOn)

    def onObjectRetrieved(retrievedObj: ObjectT): Unit = {
      objToWorkOn = retrievedObj
      if (opType == ConcreteOperationTypes.METHOD || opType == ConcreteOperationTypes.INLINE_METHOD) {
        val (newObj, newAnswer) = executeOperation(request, objToWorkOn)
        objToWorkOn = newObj
        answerToReturn = Some(newAnswer) //must overwrite
        //if method or inline method operation should not be empty
        if (request.operation.getOrElse(false).isInstanceOf[Query[_, _]]) { //todo ugly
          replyWithResponse(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
          return
        }
      }

      this.synchronized {
        logger.log(Level.INFO, "updating ohs and authenticator...")
        var updatedReplicaHistory: ReplicaHistory = myReplicaHistory.appended(lt -> ltCo) //with rh as sortedset: replicaHistory + (lt -> ltCo)
        var updatedAuthenticator = authenticateRh(updatedReplicaHistory, keysSharedWithMe) //updateAuthenticatorFor(keysSharedWithMe)(ip)(updatedReplicaHistory)
        authenticatedReplicaHistory = (updatedReplicaHistory, updatedAuthenticator)

        //overriding answer since it's ignored at client side
        if (opType == ConcreteOperationTypes.COPY) {
          answerToReturn = Option.empty
        }
        if (opType == ConcreteOperationTypes.METHOD
          || opType == ConcreteOperationTypes.INLINE_METHOD
          || opType == ConcreteOperationTypes.COPY) {

          logger.logWithPrefix(msg = "Storing updated (object, answer): (" + objToWorkOn + ", " + answerToReturn + ") with lt " + lt.time + " for request: " + request)
          storage = storage.store[AnswerT](lt, (objToWorkOn, answerToReturn))
        }

        if (opType == ConcreteOperationTypes.METHOD
          || opType == ConcreteOperationTypes.INLINE_METHOD) {
          updatedReplicaHistory = prune(updatedReplicaHistory, ltCo)
          updatedAuthenticator = authenticateRh(updatedReplicaHistory, keysSharedWithMe)
          authenticatedReplicaHistory = (updatedReplicaHistory, updatedAuthenticator)
          logger.logWithPrefix(msg = "Updated authenticated replica history is: " + authenticatedReplicaHistory)
        }
        replyWithResponse(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
      }
    }
  }


  override def sObjectRequest(request: LogicalTimestamp, //or the wrapping class
                              responseObserver: StreamObserver[ObjectSyncResponse[ObjectT]]): Unit = {
    logger.log(Level.INFO, "object sync request received")
    replyWith(ObjectSyncResponse(StatusCode.SUCCESS,
      storage.retrieveObject(request)), responseObserver)
  }

  private def replyWith[T](response: T, responseObserver: StreamObserver[T]): Unit = {
    logger.log(Level.INFO, "server " + id(RecipientInfo(ip, port)) + " sending response: " + response + ".")
    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }
}
