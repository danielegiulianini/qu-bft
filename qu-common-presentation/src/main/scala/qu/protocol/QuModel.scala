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
  override type OHS = ServerId => (ReplicaHistory, α) //cyclic dep: ReplicaHistory depends on LogicalTimestamp that depends on OHS that depends on ReplicaHistory

  //now I can add it
  //def latestTime(ohs: OHS): LogicalTimestamp = ohs.


  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires


  //def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh.
}


trait AnstractAbstractQuModel extends AbstractQuModel {
  override type OHS = Map[ServerId, (ReplicaHistory, α)] //cyclic dep: ReplicaHistory depends on LogicalTimestamp that depends on OHS that depends on ReplicaHistory

  implicit val OHSOrdering: Ordering[OHS] = (x: OHS, y: OHS) => x.toString compare y.toString

  override type LogicalTimestamp = MyLogicalTimestamp

  def latestTime(ohs: OHS): LogicalTimestamp = {
    ohs
      .values
      .map(_._1)
      .map(latestTime(_))
      .max
  }

  //what is self-independent can be put as other trait/class and plugged by mixin
  case class MyLogicalTimestamp(a: String, val barrierFlag: Boolean, val clientId: ClientId, val ohs: OHS) extends Ordered[MyLogicalTimestamp] {
    //a compare method here or a (implicit) ordering in companion object
    override def compare(that: MyLogicalTimestamp): Int = ???
  }

  def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh
    .flatMap(x => Set(x._1, x._2)) //or can use ordered/ing of candid here
    .max
}

