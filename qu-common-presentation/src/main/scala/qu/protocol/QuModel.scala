package qu.protocol


trait QuModel {
  type Operation
  type ReplicaHistory
  type HistorySet
  type OHS
  type ClientId
  type ServerId
  type Time
  type LogicalTimestamp = (Time, /*Boolean, */String, ClientId, OHS)
  type Candidate = (LogicalTimestamp, LogicalTimestamp)


  //or:   type LogicalTimestamp <: { val time; val barrierFlag;

  type OperationType
  type α  //authenticator

  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS): Candidate

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  //def latestTime(ohs: OHS): LogicalTimestamp

  def classify(ohs: OHS): (OperationType, Candidate, Candidate) //barrierflag to add
  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them...

  def compare(logicalTimestamp1: LogicalTimestamp, logicalTimestamp2: LogicalTimestamp): Int

  def max(logicalTimestamp: LogicalTimestamp): Candidate

}

//what is self-independent can be put as other trait/class and plugged by mixin

case class MyLogicalTimestamp(a:String)

trait AbstractQuModel extends QuModel {
  //cyclic dep: ReplicaHistory depends on LogicalTimestamp that depends on OHS that depends on ReplicaHistory
  override type OHS = ServerId => ReplicaHistory

  override type ReplicaHistory = Set[Candidate]

  override type Time = Int
  override type ClientId = String

  //since RH is a ordered set must define ordering for LogicalTimestamp


  //def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh.
}
