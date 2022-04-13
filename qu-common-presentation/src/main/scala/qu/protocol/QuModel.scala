package qu.protocol

import scala.collection.SortedSet


trait QuModel {
  type Operation
  type ReplicaHistory
  type HistorySet
  type OHS
  type ClientId
  type ServerId
  type Time
  //type Candidate = <: { val lt: LogicalTimestamp; val ltCo: LogicalTimestamp }
  type Candidate = (LogicalTimestamp, LogicalTimestamp)
  type LogicalTimestamp <: {val time: Int; val barrierFlag: Boolean; val clientId: ClientId; val ohs: OHS} //barrierFlag
  //type LogicalTimestamp = (Time, /*Boolean, */ String, ClientId, OHS)
  type OperationType
  type α //authenticator

  //number of replica histories in the object history set in which it appears
  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS): Candidate

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  def classify(ohs: OHS): (OperationType, Candidate, Candidate) //barrierflag to add
  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them...

  def compare(logicalTimestamp1: LogicalTimestamp, logicalTimestamp2: LogicalTimestamp): Int

  def max(logicalTimestamp: LogicalTimestamp): Candidate

}

trait AbstractQuModel extends QuModel {

  override type Time = Int

  override type ClientId = String

  override type ReplicaHistory = SortedSet[Candidate]

  //or structural type? so I can name
  //override type OHS = ServerId => (ReplicaHistory, α)

  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires
}


trait AnstractAbstractQuModel extends AbstractQuModel {
  //the ones of the following that are self-independent can be put in separate trait/class and plugged by mixin
  override type OHS = Map[ServerId, (ReplicaHistory, α)]

  //or as Ordering:   implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] = (x: MyLogicalTimestamp, y: MyLogicalTimestamp) => x.toString compare y.toString
  override type LogicalTimestamp = MyLogicalTimestamp

  //clean but not considers if rh are not ordered by server...
  //implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.values.toString compare y.toString
  implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) =>
    x.values.toString compare y.toString

  case class MyLogicalTimestamp(time: Int, barrierFlag: Boolean, clientId: ClientId, ohs: OHS) /*extends Ordered[MyLogicalTimestamp] {
    //a compare method here or a (implicit) ordering in companion object
    override def compare(that: MyLogicalTimestamp): Int =
      (self.last compare that.last) match {
        case 0 =>
          (self.first compare that.first) match {
            case 0 => self.middle compare that.middle
            case c => c
          }
        case c => c
      }
  }*/
  implicit val MyLogicalTimestampOrdering: Ordering[MyLogicalTimestamp] =
    Ordering.by(lt => (lt.time, lt.barrierFlag, lt.clientId, lt.ohs))

  def latestTime(ohs: OHS): LogicalTimestamp = {
    ohs
      .values
      .map(_._1)
      .map(latestTime)
      .max
  }

  def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh
    .flatMap(x => Set(x._1, x._2))
    .max
}

