package qu.model.examples

import qu.model.ConcreteQuModel.ServerId
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}

//commands shared between test suites
/*object SharedContainer2 {
  def generateKey(a: String, b: String): ServerId = "k" + a + b

  def keysForServer(serverId: ServerId, serversIds: Set[ServerId]): Map[ServerId, String] =
    serversIds.map(otherServerId => otherServerId -> generateKey(serverId, otherServerId)).toMap //    serverId -> serversIds.map(otherServerId => serverId -> "k" + serverId + otherServerId).toMap

  case class Scenario(thresholds: QuorumSystemThresholds,
                      serversIds: Set[ServerId]) {
    val serverKeys: Map[ServerId, Map[ConcreteQuModel.ServerId, ServerId]] =
      serversIds.map(id => id -> keysForServer(id, serversIds)).toMap
  }
  /*
  case object FourServersScenario

  (
  val thresholds: QuorumSystemThresholds = QuorumSystemThresholds(t = 1, q = 3, b = 0)
  )
*/
}*/
