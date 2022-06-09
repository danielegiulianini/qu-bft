package qu.model.examples

import Commands.Increment
import qu.model.ConcreteQuModel.{Candidate, Key, OHS, OHSRepresentation, OperationRepresentation, ReplicaHistory, ServerId, authenticateRh, emptyCandidate, emptyLT, emptyOhs, represent, ConcreteLogicalTimestamp => LT}

import scala.collection.immutable.{List => RH}

//some utilities for constructing ohs, rhs and authenticators (can also be a object of utilities
// (instead of a trait to mix)
trait OHSFixture5 {

  val aEmptyOhsRepresentation: Option[OperationRepresentation] = emptyOhsRepresentation(List())

  def emptyOhsRepresentation(servers: List[ServerId]): Some[OperationRepresentation] =
    Some(represent(emptyOhs(servers.toSet))) //Some("ohsrepr")

  val aOperationRepresentation: Some[OperationRepresentation] =
    Some(represent[Unit, Int](Some(Increment()))) //Some("oprepr")

  def aCandidate(ltTime: Int, ltCoTime: Int): Candidate = (aLt(ltTime), aLt(ltCoTime))

  def aCandidate(ltTime: Int, ltCoTime: Int, serversIds: Set[ServerId]): Candidate = (aLtWithServersIds(ltTime, serversIds = serversIds), aLtWithServersIds(ltCoTime, serversIds = serversIds))

  def aLt(time: Int, barrierFlag: Boolean = false, clientId: Option[String] =
  Some("client1"), opRepr: Option[OperationRepresentation] = aOperationRepresentation, ohsRepr: Option[OHSRepresentation] = aEmptyOhsRepresentation): LT =
    LT(time, barrierFlag, clientId, opRepr, ohsRepr)

  def aLtWithServersIds(time: Int, barrierFlag: Boolean = false, clientId: Option[String] =
  Some("client1"), opRepr: Option[OperationRepresentation] = aOperationRepresentation, serversIds: Set[ServerId]): LT =
    LT(time, barrierFlag, clientId, opRepr, emptyOhsRepresentation(serversIds.toList))

  def rhsWithBarrierFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] = {
    //order < r
    serverIds.
      zipWithIndex.map { case (sid, time) => sid -> RH(
      emptyCandidate,
      (LT(time, barrierFlag = false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))
    }.toMap
  }

  def twoCandidatesUnanimousRhsFor(serverIds: List[ServerId], barrierFlag: Boolean): Map[ServerId, ReplicaHistory] = {
    //to be a method (or a copy) order must be >= q (if order == n (like here) then order >= q)
    serverIds.map(_ -> RH(
      emptyCandidate,
      (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))).toMap
  }

  def unanimousRhsFor(serverIds: List[ServerId], candidates: RH[Candidate]): Map[ServerId, ReplicaHistory] = {
    serverIds.map(_ -> candidates).toMap
  }

  def rhsWithMethodFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] = {
    //to be a method order must be >= q (if order == n then order >= q)
    twoCandidatesUnanimousRhsFor(serverIds, barrierFlag = false)
  }

  def rhsWithInlineFor(serverIds: List[ServerId], barrierFlag: Boolean, repairableThreshold: Int): Map[ServerId, ReplicaHistory] = {
    def splitList[A](nums: List[A], n: Int): (List[A], List[A]) =
      (nums.take(n), nums.drop(n))

    //order <= r
    val (concordantServers, discordantServers) = splitList[ServerId](serverIds, repairableThreshold)
    //concordant rhs all have got time repairableThreshold + 1

    val concordantValue = repairableThreshold + 1
    val concordantRhs = concordantServers.map(_ -> RH(
      emptyCandidate,
      (LT(concordantValue, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))).toMap

    //discordant rhs all have got time 1. less than repairableThreshold + 1 ; 2. different from each other
    val discordantRhs = discordantServers.zipWithIndex.map { case (sid, time) => sid -> RH(
      emptyCandidate,
      (LT(time + 1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))
    }.toMap
    concordantRhs ++ discordantRhs
  }

  def rhsWithLatestTime(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] =
    twoCandidatesUnanimousRhsFor(serverIds, barrierFlag = true)

  def rhsWithCopyFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] =
    twoCandidatesUnanimousRhsFor(serverIds, barrierFlag = true)

  def generateOhsFromRHsAndKeys(rhs: Map[ServerId, ReplicaHistory], keys: Map[ServerId, Map[ServerId, Key]]): OHS = {
    keys.map { case (serverId, serverKeys) => serverId ->
      (rhs(serverId), authenticateRh(rhs(serverId), serverKeys))
    }
  }

  def ohsWithMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithMethodFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithInlineMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList,
      barrierFlag = false,
      repairableThreshold), serverKeys)

  def ohsWithInlineBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList,
      barrierFlag = true,
      repairableThreshold), serverKeys)

  def ohsWithBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithBarrierFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithCopyFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithCopyFor(serverKeys.keySet.toList), serverKeys)

  /*def ohsWithLatestTime(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithCopyFor(serverKeys.keySet.toList), serverKeys)*/

  def ohsWithInvalidAuthenticatorFor(ohs: OHS, serverId: ServerId): OHS = {
    ohs.map { case (sid, (rh, a)) => (sid, (rh, a.map { case (serverId2, hmac) =>
      if (serverId2 == serverId) (serverId2, "corrupted") else (serverId2, hmac)
    }))
    }
  }
}
