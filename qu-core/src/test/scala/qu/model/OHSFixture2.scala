package qu.model

import qu.model.ConcreteQuModel.{Key, OHS, ReplicaHistory, ServerId, emptyCandidate, emptyLT, updateAuthenticatorFor, α, ConcreteLogicalTimestamp => LT}

import scala.collection.immutable.{Map, List => RH}
import scala.language.postfixOps
import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => LT}

//todo can also be a object of utilities (instead of a trait to mix)
trait OHSFixture2 {
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

  def unanimousRhsFor(serverIds: List[ServerId], barrierFlag:Boolean): Map[ServerId, ReplicaHistory] = {
    //to be a method or a copy order must be >= q (if order == n then order >= q)
    serverIds.map(sid => sid -> RH(
      emptyCandidate,
      (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))).toMap
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
    //concordant rhs all have got time 1
    val concordantRhs = concordantServers.map(_ -> RH(
      emptyCandidate,
      (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))).toMap
    //discordant rhs all have got time different from 1 and different from each other
    val discordantRhs = discordantServers.zipWithIndex.map { case (sid, time) => sid -> RH(
      emptyCandidate,
      (LT(time + 2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT))
    }.toMap
    concordantRhs ++ discordantRhs
  }

  def rhsWithCopyFor(serverIds: List[ServerId]): Map[ServerId, ReplicaHistory] = {
    unanimousRhsFor(serverIds, true)
  }

  def generateOhsFromRHsAndKeys(rhs: Map[ServerId, ReplicaHistory], keys: Map[ServerId, Map[ServerId, Key]]): OHS =
    keys.map { case (id, keys) => id -> (rhs(id), updateAuthenticatorFor(keys)(id)(rhs(id))) }

  def ohsWithMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithMethodFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithInlineMethodFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList, true, repairableThreshold), serverKeys)

  def ohsWithInlineBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]], repairableThreshold: Int): OHS =
    generateOhsFromRHsAndKeys(rhsWithInlineFor(serverKeys.keySet.toList, false, repairableThreshold), serverKeys)

  def ohsWithBarrierFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithBarrierFor(serverKeys.keySet.toList), serverKeys)

  def ohsWithCopyFor(serverKeys: Map[ServerId, Map[ServerId, Key]]): OHS =
    generateOhsFromRHsAndKeys(rhsWithCopyFor(serverKeys.keySet.toList), serverKeys)

  /*
    def invalidateAuthenticatorForServer(serverKeys: Map[ServerId, Map[ServerId, Key]], serverId: ServerId, reparairbleThreshlold: Int): α = {
      val (_, originalAuthenticator) = ohsWithInlineMethodFor(serverKeys, reparairbleThreshlold)
      originalAuthenticator.map {
        case (id, _) if id == serverId => id -> "corrupted"
      }
    }
  */
  /*def ohsWithInvalidAuthenticatorFor(serverId: ServerId): OHS =
    ohsWithMethod.map { case (sid, (rh, _)) if sid == serverId => (sid, (rh, invalidateAuthenticatorForServer(sid))) }
*/

}



//  override type ReplicaHistory = SortedSet[Candidate]
//  override type α = Map[ServerId, HMAC]
//  override type AuthenticatedReplicaHistory = (ReplicaHistory, α)
//  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]