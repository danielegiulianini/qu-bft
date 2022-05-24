package qu.model

import scala.collection.SortedSet


trait QuModel {
  type Operation[T, U]
  type ReplicaHistory
  type OHS
  type ClientId
  type ServerId = String
  type Time
  type Flag = Boolean

  type OperationRepresentation
  type OHSRepresentation
  type LogicalTimestamp <: {val time: Int; val barrierFlag: Flag; val clientId: Option[ClientId]; val operation: Option[OperationRepresentation]; val ohs: Option[OHSRepresentation]} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)

  type OperationType
  type α //authenticator
  type Candidate = (LogicalTimestamp, LogicalTimestamp)

  //number of replica histories in the object history set in which it appears
  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS, barrierFlag: Flag, repairableThreshold: Int): Candidate

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  def classify(ohs: OHS,
               repairableThreshold: Int,
               quorumThreshold: Int): (OperationType, Candidate, Candidate)
}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires
  override type ReplicaHistory = SortedSet[Candidate]

}



trait AbstractAbstractQuModel extends QuModel {
  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory = SortedSet[Candidate]

  type HMAC
  override type α = Map[ServerId, HMAC]

  type AuthenticatedReplicaHistory = (ReplicaHistory, α) //case class MyAuthenticatedReplicaHistory(serverId: ServerId, authenticator: α)  //tuples are difficult to read...  //refactored since used in responses also...

  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]

  val startingTime = 0
  val emptyLT = MyLogicalTimestamp(startingTime, false, Option.empty, Option.empty, Option.empty)
  val emptyCandidate: Candidate = (emptyLT, emptyLT)
  val emptyRh: ReplicaHistory = SortedSet(emptyCandidate)

  type Key = String

  //most general
  def fillAuthenticator(keys: Map[ServerId, String])(fun: Key => HMAC): α =
    keys.view.mapValues(fun(_)).toMap

  def fillAuthenticatorFor(keys: Map[ServerId, String])(serverIdToUpdate: ServerId)(fun: (Key) => HMAC): α =
    fillAuthenticator(keys.view.filterKeys(_ == serverIdToUpdate).toMap)(fun) //fillAuthenticator(keys.filter(kv => kv._1  == serverIdToUpdate))(fun)

  val nullAuthenticator: α

  val emptyAuthenticatedRh: AuthenticatedReplicaHistory = SortedSet(emptyCandidate) -> nullAuthenticator

  def emptyOhs(serverIds: Set[ServerId]): OHS =
    serverIds.map(_ -> emptyAuthenticatedRh).toMap

  override type LogicalTimestamp = MyLogicalTimestamp //or as Ordering:   implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] = (x: MyLogicalTimestamp, y: MyLogicalTimestamp) => x.toString compare y.toString

  type OperationRepresentation = String
  type OHSRepresentation = String

  case class MyLogicalTimestamp(time: Int,
                                barrierFlag: Boolean,
                                clientId: Option[ClientId],
                                operation: Option[OperationRepresentation],
                                ohs: Option[OHSRepresentation])

  //candidate ordering leverages the implicit ordering of tuples and of MyLogicalTimestamp
  implicit def MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.ohs))

  def latestTime(ohs: OHS): LogicalTimestamp =
    ohs
      .values
      .map(_._1)
      .map(latestTime)
      .max

  def contains(replicaHistory: ReplicaHistory, candidate: Candidate) = replicaHistory(candidate)

  override def latestTime(rh: ReplicaHistory): LogicalTimestamp =
    rh
      .flatMap(x => Set(x._1, x._2)) //flattening
      .max

  override def order(candidate: (MyLogicalTimestamp, MyLogicalTimestamp),
                     ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp, MyLogicalTimestamp)], α)]): Int =
    ohs.values.count(_._1.contains(candidate))  //foreach replicahistory count if it contains the given candidate


  //todo here I need dependency injection of q
  override def latestCandidate(ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp, MyLogicalTimestamp)], α)],
                               barrierFlag: Boolean,
                               repairableThreshold: Int):
  (MyLogicalTimestamp, MyLogicalTimestamp) = {
    /*ohs
      .values
      .map(rh => rh._1.max) //I can filter by order > repairableThreshold first first (and taking the max then) or viceversa
      .filter(candidate => order(candidate, ohs) > repairableThreshold)
      .max*/
    ohs
      .values
      .flatMap(rh => rh._1)
      .filter(order(_, ohs) >= repairableThreshold)
      .max
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
    (LogicalTimestamp, LogicalTimestamp),
    (LogicalTimestamp, LogicalTimestamp)) = {
    val latestObjectVersion = latestCandidate(ohs, barrierFlag = false, repairableThreshold)
    val latestBarrierVersion = latestCandidate(ohs, barrierFlag = true, repairableThreshold)
    val ltLatest = latestTime(ohs)
    //renaming without using custom case class instead of tuples...
    val (latestObjectVersionLT, _) = latestObjectVersion
    val (latestBarrierVersionLT, _) = latestBarrierVersion

    val operationType = if (latestObjectVersionLT == ltLatest && order(latestObjectVersion, ohs) >= quorumThreshold) ConcreteOperationTypes.METHOD
    else if (latestObjectVersionLT == ltLatest && order(latestObjectVersion, ohs) >= repairableThreshold) ConcreteOperationTypes.INLINE_METHOD
    else if (latestBarrierVersionLT == ltLatest && order(latestObjectVersion, ohs) >= quorumThreshold) ConcreteOperationTypes.COPY
    else if (latestBarrierVersionLT == ltLatest && order(latestObjectVersion, ohs) >= repairableThreshold) ConcreteOperationTypes.INLINE_BARRIER
    else ConcreteOperationTypes.BARRIER
    (operationType, latestObjectVersion, latestBarrierVersion)
  }

  def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation

  def represent(OHSRepresentation: OHS): OHSRepresentation

  def setup[T, U](operation: Option[Operation[T, U]],
                  ohs: OHS,
                  quorumThreshold: Int,
                  repairableThreshold: Int,
                  clientId: String): (OperationType, Candidate, LogicalTimestamp) = {
    val (opType, latestObjectVersion, latestBarrierVersion) = classify(ohs, quorumThreshold, repairableThreshold)
    val conditionedOnLogicalTimestamp = latestObjectVersion._1 //._1 stands for lt
    if (opType == ConcreteOperationTypes.METHOD)
      ( //opType
        opType,
        //candidate
        (MyLogicalTimestamp(
          time = latestTime(ohs).time + 1,
          barrierFlag = false,
          clientId = Some(clientId),
          operation = Some(represent[T, U](operation)),
          ohs = Some(represent(ohs))),
          conditionedOnLogicalTimestamp),
        //ltCurrent
        conditionedOnLogicalTimestamp)
    else if (opType == ConcreteOperationTypes.BARRIER) {
      val lt = MyLogicalTimestamp(
        time = latestTime(ohs).time + 1,
        barrierFlag = true,
        clientId = Some(clientId),
        operation = Some(represent(operation)),
        ohs = Some(represent(ohs)))
      (opType,
        //candidate
        (lt, conditionedOnLogicalTimestamp),
        //ltCurrent
        lt)
    } else if (opType == ConcreteOperationTypes.COPY)
      (opType,
        (MyLogicalTimestamp(
          time = latestTime(ohs).time + 1,
          barrierFlag = false,
          clientId = Some(clientId),
          operation = conditionedOnLogicalTimestamp.operation,
          ohs = Some(represent(ohs))), conditionedOnLogicalTimestamp),
        //ltCurrent
        latestBarrierVersion._1)
    else if (opType == ConcreteOperationTypes.INLINE_METHOD)
      (opType,
        //candidate
        latestObjectVersion,
        //ltCurrent
        latestObjectVersion._1)
    else /*OperationType1.INLINE_BARRIER*/
      (opType,
        //candidate
        latestBarrierVersion,
        //ltCurrent
        latestBarrierVersion._2)
  }
}


//maybe more implementations (that with compact authenticators...)
object ConcreteQuModel extends AbstractAbstractQuModel with CryptoMd5Authenticator with Operations with Hashing {
  //final keyword removed to avoid https://github.com/scala/bug/issues/4440 (solved in dotty)
  case class Request[ReturnValueT, ObjectT](operation: Option[Operation[ReturnValueT, ObjectT]],
                                            ohs: OHS)

  case class Response[ReturnValueT](responseCode: StatusCode,
                                    answer: ReturnValueT,
                                    authenticatedRh: AuthenticatedReplicaHistory)


  case class ObjectSyncResponse[ObjectT](responseCode: StatusCode,
                                         answer: Option[ObjectT])
}





//Wrapping for objectSyncRequest:
//object sync request:
//1. LogicalTimestamp only
//2. wrapping class:
/*case class LogicalTimestampOperation[ReturnValueObjectT](logicalTimestamp:
                                                         LogicalTimestamp)
  extends Query[ReturnValueObjectT, ReturnValueObjectT] {
  override def compute(obj: ReturnValueObjectT): ReturnValueObjectT = obj
}*/

//object sync response:
//1. reuse response (some fields are null)
//2. new class:


/*with pattern matching:
val operationType = (latestObjectVersionLT, latestBarrierVersionLT) match {
  case (`ltLatest`, _) if order(latestObjectVersion, ohs) > quorumThreshold => 1
  case (`ltLatest`, _) if order(latestObjectVersion, ohs) > repairableThreshold => 2
  case (_, `ltLatest`) if order(latestObjectVersion, ohs) > quorumThreshold => 3
  case (_, `ltLatest`) if order(latestObjectVersion, ohs) > repairableThreshold => 4
  case _ => 5
}*/