package qu.protocol


trait QuModel {
  type Operation
  type Candidate = (LogicalTimestamp, LogicalTimestamp)
  type ReplicaHistory
  type HistorySet
  type OHS
  type ClientId
  type Time
  type LogicalTimestamp = (Time, /*Boolean, */String, ClientId, OHS)

  //or:   type LogicalTimestamp = (Time, /*Boolean, */String, ClientId, OHS)


  type OperationType
  type α  //authenticator

  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS): Candidate

  def latestTime(rh: ReplicaHistory): LogicalTimestamp

  def latestTime(ohs: OHS): LogicalTimestamp

  def classify(ohs: OHS): (OperationType, Candidate, Candidate) //barrierflag to add
  // return tuples or  case classes (c.c. that extends ? it depends if i want to access them?

  def compare(logicalTimestamp1: LogicalTimestamp, logicalTimestamp2: LogicalTimestamp): Int

  def max(logicalTimestamp: LogicalTimestamp): Candidate

}

//what is self-independent can be put as other trait/class and plugged by mixin

trait AbstractQuModel extends QuModel {
  override type ReplicaHistory = Set[LogicalTimestamp]

  type Time = Int
  override type ClientId = String

  //def latestTime(rh: ReplicaHistory): LogicalTimestamp = rh.


}
