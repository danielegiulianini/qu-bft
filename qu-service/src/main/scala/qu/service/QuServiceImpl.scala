package qu.service

import io.grpc.{BindableService, Context, ServerServiceDefinition}
import io.grpc.stub.{ServerCalls, StreamObserver}
import presentation.MethodDescriptorFactory
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.SocketAddress.id
import qu.auth.common.Constants
import qu.{AbstractSocketAddress, SocketAddress, Shutdownable}
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


//ha bisogno di tutte le info che vengono settate al momento della istanziazione
class QuServiceImpl[Transportable[_], ObjectT:TypeTag](val ip: String,
                                                       val port: Int,
                                                       val privateKey: String,
                                                       val obj: ObjectT,
                                                       val thresholds: QuorumSystemThresholds,
                                                       var storage: Storage[ObjectT],
                                                       val keysSharedWithMe: Map[ServerId, Key],
                                                       var quorumPolicy: ServerQuorumPolicy[Transportable, ObjectT])
                                                      (implicit executor: ExecutionContext) extends Shutdownable {

  import qu.LoggingUtils._

  private val logger = Logger.getLogger(classOf[QuServiceImpl[Transportable, ObjectT]].getName)
  implicit val Prefix = PrefixImpl(id(SocketAddress(ip, port)))

  import java.util.concurrent.Executors

  //scheduler for io-bound (callbacks from other servers)
  val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors + 1)) //or MoreExecutors.newDirectExecutorService
  //scheduler for cpu-bound (computing hmac, for now not used)?

  var authenticatedReplicaHistory: AuthenticatedReplicaHistory = emptyAuthenticatedRh

  storage = storage.store[ObjectT](emptyLT, (obj, Option.empty)) //must add to store the initial object (passed by param)

  def sRequest[AnswerT: TypeTag](request: Request[AnswerT, ObjectT], clientId:String)(implicit objectSyncResponseTransportable: Transportable[ObjectSyncResponse[ObjectT]],
                                                                     logicalTimestampTransportable: Transportable[LogicalTimestamp])
  : Future[Response[Option[AnswerT]]] = {


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



          if ((!authenticator.contains(id(SocketAddress(ip, port))) ||
            authenticator(id(SocketAddress(ip, port))) != hmac(keysSharedWithMe(serverId), rh)) && rh != emptyRh) { //mut use rh here! (not replicahistory)
            println("CULLLLLEDDDD, reason: not contained? " + (!authenticator.contains(id(SocketAddress(ip, port)))) + "(rh is: " + rh + ").")
            if (authenticator.contains(id(SocketAddress(ip, port))))
              println("or auth differs? " + (authenticator(id(SocketAddress(ip, port))) != hmac(keysSharedWithMe(serverId), rh)))

            (serverId, (emptyRh, authenticator))
          }
          else (serverId, (rh, authenticator)) //keep authentictor untouched (as in paper)
        }
    }

    //culling invalid Replica histories
    val updatedOhs = cullRh(request.ohs)

    val (opType, (lt, ltCo), ltCurrent) = setup(request.operation, updatedOhs, thresholds.q, thresholds.r, clientId)

    logger.logWithPrefix(Level.INFO, "SEEEEEEEEERVERRRRRRRRRRR ltCo ritornato da setup is: " + ltCo)

    //repeated request
    if (contains(myReplicaHistory, (lt, ltCo))) {

      logger.logWithPrefix(msg = "repeated request detected! from request: " + request + " (opType:  " + opType + "), operation: " + request.operation)

      val answer = if (opType != ConcreteOperationTypes.BARRIER && opType != ConcreteOperationTypes.INLINE_BARRIER) {
        val (_, answer) = storage.retrieve[AnswerT](lt).getOrElse(throw new Error("inconsistent protocol state: if in replica history must be in store too."))
        answer
      } else None

      val response = Response(StatusCode.SUCCESS, answer, authenticatedReplicaHistory)
      logger.log(Level.INFO, "repeated request detected! sending response" + response)

      //replyWithResponse(response)
      return Future(response)
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

      //replyWithResponse(Response(StatusCode.FAIL, answerToReturn, authenticatedReplicaHistory))
      return Future(Response(StatusCode.FAIL, answerToReturn, authenticatedReplicaHistory))
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

    def onObjectRetrieved(retrievedObj: ObjectT): Future[Response[Option[AnswerT]]] = {
      objToWorkOn = retrievedObj
      if (opType == ConcreteOperationTypes.METHOD || opType == ConcreteOperationTypes.INLINE_METHOD) {
        val (newObj, newAnswer) = executeOperation(request, objToWorkOn)
        objToWorkOn = newObj
        answerToReturn = Some(newAnswer) //must overwrite
        //if method or inline method operation should not be empty
        if (request.operation.getOrElse(false).isInstanceOf[Query[_, _]]) {
          return Future(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
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
        return Future(Response(StatusCode.SUCCESS, answerToReturn, authenticatedReplicaHistory))
      }
    }

    onObjectRetrieved(objToWorkOn)
  }


  def sObjectRequest(request: LogicalTimestamp): Future[ObjectSyncResponse[ObjectT]] = {
    logger.log(Level.INFO, "object sync request received")
    Future(ObjectSyncResponse(StatusCode.SUCCESS,
      storage.retrieveObject(request)))
  }

  override def shutdown(): Future[Unit] = quorumPolicy.shutdown()

  override def isShutdown: Flag = quorumPolicy.isShutdown
}
