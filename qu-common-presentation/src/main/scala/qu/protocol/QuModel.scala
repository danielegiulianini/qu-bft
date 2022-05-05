package qu.protocol

import scala.collection.SortedSet


trait QuModel {
  type Operation[T, U]
  type ReplicaHistory
  type OHS
  type ClientId
  type ServerId = String
  type Time
  type Flag = Boolean

  //before TimeStampRepr as generic
  //type LogicalTimestamp[T, U] <: {val time: Int; val barrierFlag: Flag; val clientId: ClientId; val operation: Operation[T, U]; val ohs: OHS[U]} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)
  //type Candidate[U] = (LogicalTimestamp[_, U], LogicalTimestamp[_, U]) //type Candidate = <: { val lt: LogicalTimestamp; val ltCo: LogicalTimestamp }

  type OperationRepresentation
  type OHSRepresentation
  type LogicalTimestamp <: {val time: Int; val barrierFlag: Flag; val clientId: ClientId; val operation: OperationRepresentation; val ohs: OHSRepresentation} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)


  type OperationType
  type α //authenticator
  type Candidate = (LogicalTimestamp, LogicalTimestamp) //type Candidate = <: { val lt: LogicalTimestamp; val ltCo: LogicalTimestamp }

  //number of replica histories in the object history set in which it appears
  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS, barrierFlag: Flag, repairableThreshold: Int): Candidate

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them...
  def classify(ohs: OHS,
               repairableThreshold: Int,
               quorumThreshold: Int): (OperationType, Candidate, Candidate)
}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory = SortedSet[Candidate]

  //or structural type? so I can name...
  //override type OHS = ServerId => (ReplicaHistory, α)
  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires
}


trait AbstractAbstractQuModel extends AbstractQuModel {
  //the ones of the following that are self-independent can be put in separate trait/class and plugged by mixin
  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]

  type OperationRepresentation = String
  type OHSRepresentation = String

  //tuples are difficult to read...
  case class MyAuthenticatedReplicaHistory(serverId: ServerId, authenticator: α)

  //refactored since used in responses also...
  type AuthenticatedReplicaHistory = (ReplicaHistory, α) //MyAuthenticatedReplicaHistory[U]//


  //todo required??
  def emptyOhs = Map.empty[ServerId, AuthenticatedReplicaHistory]

  trait OperationA[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): ReturnValueT
  }

  trait Query[ReturnValueT, ObjectT] extends OperationA[ReturnValueT, ObjectT]

  trait Update[ReturnValueT, ObjectT] extends OperationA[ReturnValueT, ObjectT]

  //final keyword removed to avoid https://github.com/scala/bug/issues/4440 (solved in dotty)
  case class Request[ReturnValueT, ObjectT](operation: Option[OperationA[ReturnValueT, ObjectT]],
                                            ohs: OHS)

  case class Response[ReturnValueT](responseCode: StatusCode,
                                             answer: ReturnValueT,
                                             order: Int,
                                             authenticatedRh: AuthenticatedReplicaHistory)

  //object sync request:
  //1. LogicalTimestamp only
  //2.
  case class LogicalTimestampOperation[ReturnValueObjectT](logicalTimestamp:
                                                           LogicalTimestamp)
    extends Query[ReturnValueObjectT, ReturnValueObjectT] {
    override def compute(obj: ReturnValueObjectT): ReturnValueObjectT = obj
  }

  //object sync response:
  //1. reuse response (some fields are null)
  //2.
  case class ObjectSyncResponse[ObjectT](responseCode: StatusCode,
                                         answer: ObjectT)

  override type Operation[T, U] = OperationA[T, U]

  //or as Ordering:   implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] = (x: MyLogicalTimestamp, y: MyLogicalTimestamp) => x.toString compare y.toString
  override type LogicalTimestamp = MyLogicalTimestamp

  //clean but not considers if rh are not ordered by server...
  //implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.values.toString compare y.toString
  //a def is required (instead of a val) because (generic) type params are required
  implicit def OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.values.toString compare y.toString

  case class MyLogicalTimestamp(time: Int,
                                barrierFlag: Boolean,
                                clientId: ClientId,
                                operation: OperationRepresentation,
                                ohs: OHSRepresentation)

  val startingTime = 0

  def startingLogicalTimestamp() =
    MyLogicalTimestamp(startingTime, false, null, null, null)

  def startingCandidate(): Candidate = (startingLogicalTimestamp, startingLogicalTimestamp)

  def startingRh: SortedSet[(MyLogicalTimestamp, MyLogicalTimestamp)] = SortedSet(startingCandidate())

  //candidate ordering leverages the implicit ordering of tuples and of MyLogicalTimestamp
  implicit def MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.ohs))

  //todo ordering must consider null values (starting contains null values)

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
  //foreach replicahistory count if it contains the given candidate
    ohs.values.count(_._1.contains(candidate))

  //here I need dependency injection of q
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

  override def classify(ohs: Map[ServerId, (SortedSet[(MyLogicalTimestamp, MyLogicalTimestamp)], α)],
                        repairableThreshold: Int,
                        quorumThreshold: Int):
  (OperationType,
    (MyLogicalTimestamp, MyLogicalTimestamp),
    (MyLogicalTimestamp, MyLogicalTimestamp)) = {
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

  //todo are type param really needed?
  def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation

  def represent(OHSRepresentation: OHS): OHSRepresentation

  def setup[T, U](operation: Option[Operation[T, U]],
                  ohs: OHS,
                  quorumThreshold: Int,
                  repairableThreshold: Int,
                  clientId: String): (OperationType, Candidate, LogicalTimestamp) = {
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
          operation = represent[T, U](operation),
          ohs = represent(ohs)),
          conditionedOnLogicalTimestamp),
        //ltCurrent
        conditionedOnLogicalTimestamp)
    else if (opType == OperationType1.BARRIER) {
      val lt = MyLogicalTimestamp(
        time = latestTime(ohs).time + 1,
        barrierFlag = true,
        clientId = clientId,
        operation = represent(operation),
        ohs = represent(ohs))
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
          ohs = represent(ohs)), conditionedOnLogicalTimestamp),
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

  case class MyNone()

  def nullOperation[U] = new OperationA[MyNone, U] {
    override def compute(obj: U): MyNone = MyNone()
  }

}

trait CryptoMd5Authenticator {
  self: AbstractAbstractQuModel => //needs the ordering defined by SortedSet

  type α = Map[ServerId, HMAC] //SortedSet[HMAC] //or map?

  type HMAC = String

  import com.roundeights.hasher.Implicits._ // import com.roundeights.hasher.Digest.digest2string

  //leveraging sortedSet ordering here
  def hmac(key: String, replicaHistory: ReplicaHistory): HMAC =
  //should be taken over the hash of a replicahistory
    replicaHistory.toString().hmac(key).md5

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

  private def hashObject(obj: Any) = obj.toString().md5.hex

}

//could be a class separated from QUModel, bound (for LogicalTimestamp) to a ConcreteImplementation
trait Storage {
  self: AbstractQuModel =>

  def store[T, U](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, T)): Unit

  def retrieve[T, U](logicalTimestamp: LogicalTimestamp): (U, T)

}


trait GeneralStorage {
  def store[T, U](logicalTimestamp: T, objectAndAnswer: U): Unit

  def retrieve[T, U](logicalTimestamp: T): U

}

trait InMemoryStorage extends Storage {
  self: AbstractQuModel =>

  def store[T, U](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, T)): Unit = ???

  def retrieve[T, U](logicalTimestamp: LogicalTimestamp): (U, T) =  ???

}

trait PersistentStorage extends Storage {
  self: AbstractQuModel =>

}

trait PersistentCachingStorage extends Storage {
  self: AbstractQuModel =>

}


//maybe more implementations (that with compact authenticators...)
object ConcreteQuModel extends AbstractAbstractQuModel with CryptoMd5Authenticator with InMemoryStorage
