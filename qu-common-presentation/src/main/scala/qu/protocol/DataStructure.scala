package qu.protocol


trait DataStructure {
  type Operation
  type Candidate
  type HistorySet
  type OHS
  type LogicalTimestamp
  type OperationType

  def order(candidate: Candidate, ohs: OHS): Int

  def latestCandidate(ohs: OHS): Candidate

  def latestTime(ohs: OHS): LogicalTimestamp

  def classify(ohs: OHS): (OperationType, Candidate, Candidate) //barrierflag to add  //ritorno delle tuple o delle case class mie??

  def compare(logicalTimestamp1: LogicalTimestamp, logicalTimestamp2: LogicalTimestamp) : Int

  def max(logicalTimestamp: LogicalTimestamp): Candidate

}
