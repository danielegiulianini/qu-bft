package qu.model

import qu.model.ConcreteQuModel.{
  Candidate, Key, OHS, ReplicaHistory, ServerId, authenticateRh, emptyCandidate, emptyLT,
  α, ConcreteLogicalTimestamp => LT
}

import scala.collection.immutable.{Map, List => RH}
import scala.language.postfixOps

//todo can also be a object of utilities (instead of a trait to mix)
trait OHSFixture {
  //some utilities for constructing ohs, rhs and authenticators

  def emptyOhsRepresentation(servers: List[ServerId]): Some[Key] = Some("ohsrepr") //represent(emptyOhs(servers)))

  val aOperationRepresentation: Some[Key] = Some("oprepr") //represent[Int, Int](Some(new GetObj[Int]())))

  def rhsWithBarrierFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] = {
    //order < r
    serverIds.
      zipWithIndex.map { case (sid, time) => sid -> RH(
      emptyCandidate,
      (LT(time, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))
    }.toMap
  }

  def unanimousRhsFor(serverIds: List[ServerId], barrierFlag: Boolean): Map[ServerId, ReplicaHistory] = {
    //to be a method (or a copy) order must be >= q (if order == n (like here) then order >= q)
    serverIds.map(sid => sid -> RH(
      emptyCandidate,
      (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))).toMap
  }

  def unanimousRhsFor(serverIds: List[ServerId], candidates: RH[Candidate]): Map[ServerId, ReplicaHistory] = {
    serverIds.map(sid => sid -> candidates).toMap
  }

  def rhsWithMethodFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] = {
    //to be a method order must be >= q (if order == n then order >= q)
    unanimousRhsFor(serverIds, false)
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
    unanimousRhsFor(serverIds, true)

  def rhsWithCopyFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] =
    unanimousRhsFor(serverIds, true)

  def generateOhsFromRHsAndKeys(rhs: Map[ServerId, ReplicaHistory], keys: Map[ServerId, Map[ServerId, Key]]): OHS = {
    keys.map { case (serverId, serverKeys) => serverId ->
      (rhs(serverId), authenticateRh(rhs(serverId), serverKeys))
    }
  }

  def ohsWithMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithMethodFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithInlineMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList,
      false,
      repairableThreshold), serverKeys)

  def ohsWithInlineBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList,
      true,
      repairableThreshold), serverKeys)

  def ohsWithBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithBarrierFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithCopyFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithCopyFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithLatestTime(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithCopyFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithInvalidAuthenticatorFor(ohs: OHS, serverId: ServerId): OHS = {
    ohs.map { case (sid, (rh, a)) => (sid, (rh, a.map { case (serverId2, hmac) =>
      if (serverId2 == serverId) (serverId2, "corrupted") else (serverId2, hmac )})) }
  }
}



//  override type ReplicaHistory = SortedSet[Candidate]
//  override type α = Map[ServerId, HMAC]
//  override type AuthenticatedReplicaHistory = (ReplicaHistory, α)
//  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]


/*
  def invalidateAuthenticatorForServer(serverKeys: Map[ServerId, Map[ServerId, Key]],
                                       serverId: ServerId,
                                       reparairbleThreshlold: Int): α = {
    val (_, originalAuthenticator) = ohsWithInlineMethodFor(serverKeys, reparairbleThreshlold)
    originalAuthenticator.map {
      case (id, _) if id == serverId => id -> "corrupted"
    }

    keys.map { case (serverId, serverKeys) => serverId ->
      (rhs(serverId), authenticateRh(rhs(serverId), serverKeys))
    }
  }

  def ohsWithInvalidAuthenticatorFor2(ohs: OHS, serverId: ServerId): OHS = {
    //lascio quella che c'è altrimenti
    ohs.map { case (sid, (rh, _)) if sid == serverId => (sid, (rh, invalidateAuthenticatorForServer(sid))) }
  }*/