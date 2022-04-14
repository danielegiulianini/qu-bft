package qu.protocol

import scala.collection.SortedSet


trait QuModel {
  type Operation[T, U]
  type ReplicaHistory[U]
  type OHS[U]
  type ClientId
  type ServerId
  type Time
  //type Candidate = <: { val lt: LogicalTimestamp; val ltCo: LogicalTimestamp }
  type Candidate[U] = (LogicalTimestamp[_, U], LogicalTimestamp[_, U])
  type LogicalTimestamp[T, U] <: {val time: Int; val barrierFlag: Boolean; val clientId: ClientId; val operation: Operation[T, U]; val ohs: OHS[U]} // this causes cyc dep: type LogicalTimestamp = (Time, Boolean, String, ClientId, OHS)

  type OperationType

  type α //authenticator

  //number of replica histories in the object history set in which it appears
  def order[U](candidate: Candidate[U], ohs: OHS[U]): Int

  def latestCandidate[U](ohs: OHS[U]): Candidate[U]

  def latestTime[U](rh: ReplicaHistory[U]): LogicalTimestamp[_, U]

  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them...
  def classify[U](ohs: OHS[U]): (OperationType, Candidate[U], Candidate[U]) //barrierflag to add

  def compare[U](logicalTimestamp1: LogicalTimestamp[_, U], logicalTimestamp2: LogicalTimestamp[_, U]): Int

  def max[U](logicalTimestamp: LogicalTimestamp[_, U]): Candidate[U]

}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory[U] = SortedSet[Candidate[U]]

  //or structural type? so I can name
  //override type OHS = ServerId => (ReplicaHistory, α)
  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires
}


trait AnstractAbstractQuModel extends AbstractQuModel {
  //the ones of the following that are self-independent can be put in separate trait/class and plugged by mixin
  override type OHS[U] = Map[ServerId, (ReplicaHistory[U], α)]
  override type Operation[T, U] = Messages.Operation[T, U]

  //or as Ordering:   implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] = (x: MyLogicalTimestamp, y: MyLogicalTimestamp) => x.toString compare y.toString
  override type LogicalTimestamp[T, U] = MyLogicalTimestamp[T, U]

  //clean but not considers if rh are not ordered by server...
  //implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.values.toString compare y.toString
  implicit def OHSOrdering[U]: Ordering[OHS[U]] = (x: OHS[U], y: OHS[U]) => x.values.toString compare y.toString

  case class MyLogicalTimestamp[T, U](time: Int,
                                      barrierFlag: Boolean,
                                      clientId: ClientId,
                                      operation: Messages.Operation[T, U],
                                      ohs: OHS[U])

  implicit def MyLogicalTimestampOrdering[U]: Ordering[MyLogicalTimestamp[_, U]] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.ohs))

  def latestTime[U](ohs: OHS[U]): LogicalTimestamp[_, U] = {
    ohs
      .values
      .map(_._1)
      .map(latestTime[U])
      .max
  }

  def latestTime[U](rh: ReplicaHistory[U]): LogicalTimestamp[_, U] = rh
    .flatMap(x => Set(x._1, x._2))
    .max
}

