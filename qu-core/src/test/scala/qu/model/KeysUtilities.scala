package qu.model

import qu.model.QuorumSystemThresholdQuModel.ServerId

//commands shared between test suites
trait KeysUtilities {

  def generateKey(a: String, b: String): ServerId = "k" + a + b

  def keysForServer(serverId: ServerId, serversIds: Set[ServerId]): Map[ServerId, String] =
    serversIds.map(otherServerId => otherServerId -> generateKey(serverId, otherServerId)).toMap
}
