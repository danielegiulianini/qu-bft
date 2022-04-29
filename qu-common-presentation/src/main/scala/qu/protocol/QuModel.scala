package qu.protocol

import scala.collection.SortedSet


trait QuModel {
  type Operation[T, U]
  type ReplicaHistory[U]
  type OHS[U]
  type ClientId
  type ServerId
  type Time
  type Candidate[U] = (LogicalTimestamp[_, U], LogicalTimestamp[_, U]) //type Candidate = <: { val lt: LogicalTimestamp; val ltCo: LogicalTimestamp }
  type Flag = Boolean
  type LogicalTimestamp[T, U] <: {val time: Int; val barrierFlag: Flag; val clientId: ClientId; val operation: Operation[T, U]; val ohs: OHS[U]} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)
  type OperationType
  type α //authenticator

  //number of replica histories in the object history set in which it appears
  def order[U](candidate: Candidate[U], ohs: OHS[U]): Int

  def latestCandidate[U](ohs: OHS[U], barrierFlag: Flag, repairableThreshold: Int): Candidate[U]

  def latestTime[U](rh: ReplicaHistory[U]): LogicalTimestamp[_, U]

  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them...
  def classify[U](ohs: OHS[U],
                  repairableThreshold: Int,
                  quorumThreshold: Int): (OperationType, Candidate[U], Candidate[U])
}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory[U] = SortedSet[Candidate[U]]

  //or structural type? so I can name...
  //override type OHS = ServerId => (ReplicaHistory, α)
  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires
}


trait AbstractAbstractQuModel extends AbstractQuModel {
  //the ones of the following that are self-independent can be put in separate trait/class and plugged by mixin
  override type OHS[U] = Map[ServerId, AuthenticatedReplicaHistory[U]]

  //refactored since used in responses also...
  type AuthenticatedReplicaHistory[U] = (ReplicaHistory[U], α)

  //todo required??
  def emptyOhs[U] = Map.empty[ServerId, U]

  trait OperationA[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): ReturnValueT
  }

  trait Query[ReturnValueT, ObjectT] extends OperationA[ReturnValueT, ObjectT]

  trait Update[ReturnValueT, ObjectT] extends OperationA[ReturnValueT, ObjectT]

  final case class Request[ReturnValueT, ObjectT](operation: OperationA[ReturnValueT, ObjectT],
                                                  ohs: OHS[ObjectT])

  final case class Response[ReturnValueT, ObjectT](responseCode: StatusCode,
                                                   answer: ReturnValueT,
                                                   order: Int,
                                                   authenticatedRh: AuthenticatedReplicaHistory[ObjectT])


  //object sync request:
  //1. LogicalTimestamp only
  //2.
  case class LogicalTimestampOperation[ReturnValueObjectT](logicalTimestamp:
                                                           LogicalTimestamp[ReturnValueObjectT, ReturnValueObjectT])
    extends Query[ReturnValueObjectT, ReturnValueObjectT] {
    override def compute(obj: ReturnValueObjectT): ReturnValueObjectT = obj
  }

  //object sync response:
  //1. reuse response (some fields are null)
  //2.
  final case class ObjectSyncResponse[ObjectT](responseCode: StatusCode,
                                               answer: ObjectT)

  override type Operation[T, U] = OperationA[T, U]

  //or as Ordering:   implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] = (x: MyLogicalTimestamp, y: MyLogicalTimestamp) => x.toString compare y.toString
  override type LogicalTimestamp[T, U] = MyLogicalTimestamp[T, U]

  //clean but not considers if rh are not ordered by server...
  //implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.values.toString compare y.toString
  //a def is required (instead of a val) because (generic) type params are required
  implicit def OHSOrdering[U]: Ordering[OHS[U]] = (x: OHS[U], y: OHS[U]) => x.values.toString compare y.toString

  case class MyLogicalTimestamp[T, U](time: Int,
                                      barrierFlag: Boolean,
                                      clientId: ClientId,
                                      operation: OperationA[T, U],
                                      ohs: OHS[U])

  val startingTime = 0

  def startingLogicalTimestamp[U]() =
    MyLogicalTimestamp[Null, U](startingTime, false, null, null, null)

  def startingCandidate[U](): Candidate[U] = (startingLogicalTimestamp[U], startingLogicalTimestamp[U])

  def startingRh[U]: SortedSet[(MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U])] = SortedSet(startingCandidate[U]())

  //candidate ordering leverages the implicit ordering of tuples and of MyLogicalTimestamp
  implicit def MyLogicalTimestampOrdering[U]: Ordering[MyLogicalTimestamp[_, U]] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.ohs))

  //todo ordering must consider null values (starting contains null values)

  def latestTime[U](ohs: OHS[U]): LogicalTimestamp[_, U] =
    ohs
      .values
      .map(_._1)
      .map(latestTime[U])
      .max


  override def latestTime[U](rh: ReplicaHistory[U]): LogicalTimestamp[_, U] =
    rh
      .flatMap(x => Set(x._1, x._2)) //flattening
      .max

  override def order[U](candidate: (MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U]),
                        ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U])], α)]): Int =
  //foreach replicahistory count if it contains the given candidate
    ohs.values.count(_._1.contains(candidate))

  //here I need dependency injection of q
  override def latestCandidate[U](ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U])], α)],
                                  barrierFlag: Boolean,
                                  repairableThreshold: Int):
  (MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U]) = {
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
  sealed trait OperationType1

  object OperationType1 {
    case object METHOD extends OperationType1

    case object INLINE_METHOD extends OperationType1

    case object COPY extends OperationType1

    case object BARRIER extends OperationType1

    case object INLINE_BARRIER extends OperationType1
  }

  sealed trait StatusCode

  object StatusCode {
    case object SUCCESS extends StatusCode

    case object FAIL extends StatusCode
  }


  override type OperationType = OperationType1

  override def classify[U](ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U])], α)],
                           repairableThreshold: Int,
                           quorumThreshold: Int):
  (OperationType,
    (MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U]),
    (MyLogicalTimestamp[_, U], MyLogicalTimestamp[_, U])) = {
    val latestObjectVersion = latestCandidate(ohs, barrierFlag = false, repairableThreshold)
    val latestBarrierVersion = latestCandidate(ohs, barrierFlag = true, repairableThreshold)
    val ltLatest = latestTime(ohs)
    //renaming without using custom case class instead of tuples...
    val (latestObjectVersionLT, _) = latestObjectVersion
    val (latestBarrierVersionLT, _) = latestBarrierVersion
    /*val operationType = (latestObjectVersionLT, latestBarrierVersionLT) match {
      case (`ltLatest`, _) if order(latestObjectVersion, ohs) > quorumThreshold => 1
      case (`ltLatest`, _) if order(latestObjectVersion, ohs) > repairableThreshold => 2
      case (_, `ltLatest`) if order(latestObjectVersion, ohs) > quorumThreshold => 3
      case (_, `ltLatest`) if order(latestObjectVersion, ohs) > repairableThreshold => 4
      case _ => 5
    }*/
    val operationType = if (latestObjectVersionLT == ltLatest && order(latestObjectVersion, ohs) >= quorumThreshold) OperationType1.METHOD
    else if (latestObjectVersionLT == ltLatest && order(latestObjectVersion, ohs) >= repairableThreshold) OperationType1.INLINE_METHOD
    else if (latestBarrierVersionLT == ltLatest && order(latestObjectVersion, ohs) >= quorumThreshold) OperationType1.COPY
    else if (latestBarrierVersionLT == ltLatest && order(latestObjectVersion, ohs) >= repairableThreshold) OperationType1.INLINE_BARRIER
    else OperationType1.BARRIER
    (operationType, latestObjectVersion, latestBarrierVersion)
  }

  //to be del
  type ProbingPolicy = Object => Set[ServerId]

  def setup[T, U](operation: Operation[T, U],
                  ohs: OHS[U],
                  quorumThreshold: Int,
                  repairableThreshold: Int,
                  clientId: String): (OperationType, Candidate[U], LogicalTimestamp[_, U]) = {
    val (opType, latestObjectVersion, latestBarrierVersion) = classify(ohs, quorumThreshold, repairableThreshold)
    val conditionedOnLogicalTimestamp = latestObjectVersion._1 //._1 stands for lt
    if (opType == OperationType1.METHOD)
      ( //opType
        opType,
        //candidate
        (MyLogicalTimestamp(
          time = latestTime(ohs).time + 1,
          barrierFlag = false,
          clientId = clientId,
          operation = operation,
          ohs = ohs),
          conditionedOnLogicalTimestamp),
        //ltCurrent
        conditionedOnLogicalTimestamp)
    else if (opType == OperationType1.BARRIER) {
      val lt = MyLogicalTimestamp(
        time = latestTime(ohs).time + 1,
        barrierFlag = true,
        clientId = clientId,
        operation = null,
        ohs = ohs)
      (opType,
        //candidate
        (lt, conditionedOnLogicalTimestamp),
        //ltCurrent
        lt)
    } else if (opType == OperationType1.COPY)
      (opType,
        (MyLogicalTimestamp(
          time = latestTime(ohs).time + 1,
          barrierFlag = false,
          clientId = clientId,
          operation = conditionedOnLogicalTimestamp.operation,
          ohs = ohs), conditionedOnLogicalTimestamp),
        //ltCurrent
        latestBarrierVersion._1)
    else if (opType == OperationType1.INLINE_METHOD)
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

trait CryptoMd5Authenticator {
  self: AbstractQuModel => //needs the ordering defined by SortedSet

  override type α = String

  import com.roundeights.hasher.Implicits._
  // import com.roundeights.hasher.Digest.digest2string

  //leveraging sortedSet ordering here
  def hmac[U](key: String, replicaHistory: ReplicaHistory[U]): α =
    replicaHistory.toString().hmac(key).md5

}

trait Persistence {
  self: AbstractQuModel =>

  def store[T, U](logicalTimestamp: LogicalTimestamp[T, U], objectAndAnswer: (U, T)): Unit

  def retrieve[T, U](logicalTimestamp: LogicalTimestamp[T, U]): (U, T)

}

trait PersistenceImpl extends Persistence {
  self: AbstractQuModel =>

  def store[T, U](logicalTimestamp: LogicalTimestamp[T, U], objectAndAnswer: (U, T)): Unit = {}

  def retrieve[T, U](logicalTimestamp: LogicalTimestamp[T, U]): (U, T) = null

}


//maybe more implementations (that with compact authenticators...)
object ConcreteQuModel extends AbstractAbstractQuModel with CryptoMd5Authenticator with PersistenceImpl