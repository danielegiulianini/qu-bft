package qu.model

import qu.model.QuorumSystemThresholdQuModel.ConcreteLogicalTimestamp
import qu.model.StatusCode.StatusCode

import scala.collection.SortedSet
import scala.math.Ordered.orderingToOrdered

/**
 * Completely abstract definition of Q/U model core functions and data structures.
 * The final model is a progressive refinement of this model making it more and more
 * complete by leveraging "gradual interface-implementation pattern". To
 * 1. reuse base behaviours and abstractions and
 * 2. provide type safety by preventing that different implementations are mixed
 * it is applied not just to classes but to the whole Q/U system containing abstract types too,
 * exploiting the so called "family polymorphism" that scala enables.
 */
trait QuModel {
  type Operation[T, U]

  /**
   * Replicated-object' versions history, made up of candidates.
   */
  type ReplicaHistory
  /**
   * A collection of [[ReplicaHistory]] indexed by server. It represents the client's partial observation
   * of global system state at some point in time.
   */
  type OHS
  type ClientId
  type ServerId = String
  type Time
  type Flag = Boolean

  /**
   * The representation of a [[Operation]] to be put inside a [[LogicalTimestamp]].
   */
  type OperationRepresentation

  /**
   * The representation of a [[OHS]] to be put inside a [[LogicalTimestamp]].
   */
  type OHSRepresentation
  /**
   * The logical timestamp defining the ordering between operation invocations and corresponding object versions.
   */
  type LogicalTimestamp <: {val time: Int; val barrierFlag: Flag; val clientId: Option[ClientId]; val operation: Option[OperationRepresentation]; val ohs: Option[OHSRepresentation]} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)

  type OperationType
  type authenticator
  type Candidate = (LogicalTimestamp, LogicalTimestamp)

  /**
   * The occurrences of a candidate in the object history set in which it appears.
   * @param candidate the candidate whose order is to be compute.
   * @param ohs the object history set containing the candidate.
   * @return the occurrences of the candidate.
   */
  def order(candidate: Candidate, ohs: OHS): Int


  /**
   * The latest, i.e., most recent candidate in the logical time, of type barrier or not, contained in the OHS.
   * @param ohs the OHS to classify.
   * @param barrierFlag the flag indicating if the latest barrier candidate must be returned.
   * @param repairableThreshold size of "repairable" set.
   */
  def latestCandidate(ohs: OHS, barrierFlag: Flag, repairableThreshold: Int): Option[Candidate]  //returns option since barrierFlag and repairableThreshold can be too restrictive

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  /**
   * Contains the classification logic based on a recursive Quorum thresholds systems.
   * @param ohs the OHS to classify.
   * @param repairableThreshold size of "repairable" set.
   * @param quorumThreshold quorum size, i.e. the count of replicas that makes up a quorum.
   */
  def classify(ohs: OHS,
               repairableThreshold: Int,
               quorumThreshold: Int): (OperationType, Option[Candidate], Option[Candidate])
}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  //since RH is a ordered set must define ordering for LogicalTimestamp too
  override type ReplicaHistory = SortedSet[Candidate]

}


trait LessAbstractQuModel extends QuModel {
  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory = List[Candidate]

  type hMac

  override type authenticator = Map[ServerId, hMac]

  type AuthenticatedReplicaHistory = (ReplicaHistory, authenticator)

  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]

  val startingTime = 0
  val emptyLT: ConcreteLogicalTimestamp = ConcreteLogicalTimestamp(startingTime, barrierFlag = false, Option.empty, Option.empty, Option.empty) //false because it must allow client to exit from repairing (if al servers sent the empty ohs)
  val emptyCandidate: Candidate = (emptyLT, emptyLT)
  val emptyRh: ReplicaHistory = List(emptyCandidate)

  type Key = String

  def nullAuthenticator(): authenticator

  val emptyAuthenticatedRh: AuthenticatedReplicaHistory = (emptyRh, nullAuthenticator()) //emptyRh -> nullAuthenticator

  def emptyOhs(serverIds: Set[ServerId]): OHS =
    serverIds.map(_ -> emptyAuthenticatedRh).toMap

  override type LogicalTimestamp = ConcreteLogicalTimestamp

  type OperationRepresentation = String
  type OHSRepresentation = String


  //candidate ordering leverages the implicit ordering of tuples and of MyLogicalTimestamp
  implicit def ConcreteTimestampOrdering: Ordering[ConcreteLogicalTimestamp] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.operation, lt.ohs))

  def latestTime(ohs: OHS): LogicalTimestamp = {
    ohs
      .values
      .map(_._1)
      .map(latestTime)
      .max
  }

  def contains(replicaHistory: ReplicaHistory, candidate: Candidate): Flag =
    replicaHistory.contains(candidate)

  override def latestTime(rh: ReplicaHistory): LogicalTimestamp =
    rh
      .flatMap(x => Set(x._1, x._2)) //flattening
      .max

  override def order(candidate: (ConcreteLogicalTimestamp, ConcreteLogicalTimestamp),
                     ohs: OHS): Int =
    ohs.values.count(_._1.contains(candidate)) //foreach replica history count if it contains the given candidate

  //Option returned as it might not exist (if requesting latest barrier candidate on the emptyOhs)
  override def latestCandidate(ohs: OHS,
                               barrierFlag: Boolean,
                               repairableThreshold: Int):
  Option[(ConcreteLogicalTimestamp, ConcreteLogicalTimestamp)] = {
    ohs
      .values //authenticated rhs here
      .flatMap(rh => rh._1) //candidates of rhs here
      .filter { case (lt, _) => lt.barrierFlag == barrierFlag }
      .filter(order(_, ohs) >= repairableThreshold)
      .maxOption
  }


  //classify can be plugged after definition of operationTypes instances
  sealed trait ConcreteOperationTypes

  object ConcreteOperationTypes {
    case object METHOD extends ConcreteOperationTypes

    case object INLINE_METHOD extends ConcreteOperationTypes

    case object COPY extends ConcreteOperationTypes

    case object BARRIER extends ConcreteOperationTypes

    case object INLINE_BARRIER extends ConcreteOperationTypes
  }

  override type OperationType = ConcreteOperationTypes

  override def classify(ohs: OHS,
                        repairableThreshold: Int,
                        quorumThreshold: Int):
  (OperationType,
    Option[Candidate],
    Option[Candidate]) = {

    val latestObjectVersion = latestCandidate(ohs, barrierFlag = false, repairableThreshold)
    val latestBarrierVersion = latestCandidate(ohs, barrierFlag = true, repairableThreshold)
    val ltLatest = latestTime(ohs)
    val opType = (latestObjectVersion, latestBarrierVersion) match {
      //before the most restrictive (pattern matching implicitly breaks)
      case (Some((objectLt, objectLtCo)), _) if objectLt == ltLatest & order((objectLt, objectLtCo), ohs) >= quorumThreshold => ConcreteOperationTypes.METHOD
      case (Some((objectLt, objectLtCo)), _) if objectLt == ltLatest & order((objectLt, objectLtCo), ohs) >= repairableThreshold =>
        ConcreteOperationTypes.INLINE_METHOD
      case (_, Some((barrierLt, barrierLtCo))) if barrierLt == ltLatest & order((barrierLt, barrierLtCo), ohs) >= quorumThreshold => ConcreteOperationTypes.COPY
      case (_, Some((barrierLt, barrierLtCo))) if barrierLt == ltLatest & order((barrierLt, barrierLtCo), ohs) >= repairableThreshold => ConcreteOperationTypes.INLINE_BARRIER
      case _ => ConcreteOperationTypes.BARRIER
    }
    (opType, latestObjectVersion, latestBarrierVersion)
  }

  def prune(rh: ReplicaHistory, ltCoOfMostRecentUpdate: LogicalTimestamp): ReplicaHistory =
    rh.filter { case (lt, _) =>
      lt >= ltCoOfMostRecentUpdate
    }

  def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation

  def represent(OHSRepresentation: OHS): OHSRepresentation

  def setup[T, U](operation: Option[Operation[T, U]],
                  ohs: OHS,
                  quorumThreshold: Int,
                  repairableThreshold: Int,
                  clientId: String): (OperationType, Candidate, LogicalTimestamp) = {
    val (opType, latestObjectVersion, latestBarrierVersion) = classify(ohs, repairableThreshold, quorumThreshold)
    val latestObjectVersionFilled = latestObjectVersion.getOrElse(
      throw new Exception("with a correct quorum system thresholds setting a ohs should always a latest object candidate"))
    val (conditionedOnLogicalTimestamp, _) = latestObjectVersionFilled
    if (opType == ConcreteOperationTypes.METHOD) {
      ( //opType
        opType,
        //candidate
        (ConcreteLogicalTimestamp(
          time = latestTime(ohs).time + 1,
          barrierFlag = false,
          clientId = Some(clientId),
          operation = Some(represent[T, U](operation)),
          ohs = Some(represent(ohs))),
          conditionedOnLogicalTimestamp),
        //ltCurrent
        conditionedOnLogicalTimestamp
      )
    }

    else if (opType == ConcreteOperationTypes.BARRIER) {
      val lt = ConcreteLogicalTimestamp(
        time = latestTime(ohs).time + 1,
        barrierFlag = true,
        clientId = Some(clientId),
        operation = Option.empty,
        ohs = Some(represent(ohs)))
      (opType,
        //candidate
        (lt, conditionedOnLogicalTimestamp),
        //ltCurrent
        lt)
    } else if (opType == ConcreteOperationTypes.INLINE_METHOD)
      (opType,
        latestObjectVersionFilled,
        conditionedOnLogicalTimestamp) else {
      val (latestBarrierVersionLt, latestBarrierVersionLtCo) = latestBarrierVersion.getOrElse(
        throw new Exception("with a correct quorum system thresholds setting a ohs with a operation type classified as COPY or INLINE_BARRIER should always a latest barrier candidate"))
      if (opType == ConcreteOperationTypes.COPY) {
        (opType,
          (ConcreteLogicalTimestamp(
            time = latestTime(ohs).time + 1,
            barrierFlag = false,
            clientId = Some(clientId),
            operation = conditionedOnLogicalTimestamp.operation,
            ohs = Some(represent(ohs))), conditionedOnLogicalTimestamp),
          //ltCurrent
          latestBarrierVersionLt)
      }
      else /*INLINE_BARRIER*/ {
        (opType,
          //candidate
          (latestBarrierVersionLt, latestBarrierVersionLtCo),
          //ltCurrent
          latestBarrierVersionLt)
      }
    }
  }
}

/**
 * A [[QuModel]] implementation with compact timestamp optimization and leveraging HMAC for replica History integrity.
 */
object QuorumSystemThresholdQuModel extends LessAbstractQuModel with CryptoMd5Authenticator with Operations with Hashing {

  /**
   * A client request for performing operations on the replicated object.
   * @param operation the operation the client wants to perform on the replicated object.
   * @param ohs the operation's conditioned-on object history set.
   * @tparam ReturnValueT the type of the value returned by the operation invocation.
   * @tparam ObjectT the ype of the object replicated by Q/U servers on which operations are to be submitted.
   */
  //final keyword removed to avoid https://github.com/scala/bug/issues/4440 (solved in dotty)
  case class Request[ReturnValueT, ObjectT](operation: Option[Operation[ReturnValueT, ObjectT]],
                                            ohs: OHS)

  /**
   * A replica response to client operation-performing request.
   * @param responseCode the response code stating the outcome of the client request.
   * @param answer the value returned by the operation invocation.
   * @param authenticatedRh the authenticated replica history returned by the sender replica resulting from operation
   *                        submission.
   * @tparam ReturnValueT the type of the value returned by the operation invocation.
   */
  case class Response[ReturnValueT](responseCode: StatusCode,
                                    answer: ReturnValueT,
                                    authenticatedRh: AuthenticatedReplicaHistory)

  /**
   * A replica response to replica object-sync-request for retrieving an object version it doesn't store locally.
   * @param responseCode the response code stating the outcome of the replica request.
   * @param answer the answer of the receiver replica containing the requested object or not.
   * @tparam ObjectT the ype of the object replicated by Q/U servers on which operations are to be submitted.
   */
  case class ObjectSyncResponse[ObjectT](responseCode: StatusCode,
                                         answer: Option[ObjectT])

  case class ConcreteLogicalTimestamp(time: Int,
                                      barrierFlag: Boolean,
                                      clientId: Option[ClientId],
                                      operation: Option[OperationRepresentation],
                                      ohs: Option[OHSRepresentation]) {
    override def productPrefix = "LT" //for pretty printing
  }

}



