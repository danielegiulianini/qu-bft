package qu.protocol


trait QuModel {
  type Operation
  type ReplicaHistory
  type HistorySet
  type OHS
  type ClientId
  type ServerId
  type Time
  type Candidate = (LogicalTimestamp, LogicalTimestamp)
  type LogicalTimestamp <: { val time: Int; val ohs: OHS }  //barrierFlag
  //type LogicalTimestamp = (Time, /*Boolean, */ String, ClientId, OHS)
  type OperationType
  type α //authenticator

  //number of replica histories in the object history set in which it appears
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
case class MyLogicalTimestamp(a: String)

trait AbstractQuModel extends QuModel {
  //or structural type? so naming
  override type OHS = ServerId => (ReplicaHistory, α)  //cyclic dep: ReplicaHistory depends on LogicalTimestamp that depends on OHS that depends on ReplicaHistory

  override type ReplicaHistory = Set[Candidate]

  override type Time = Int

  override type ClientId = String

  //now I can add it
  //def latestTime(ohs: OHS): LogicalTimestamp =


  //since RH is a ordered set must define ordering for LogicalTimestamp, that actually requires


  //def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh.
}




