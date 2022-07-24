package qu.model

import qu.model.QuorumSystemThresholdQuModel.{Key, ServerId}


trait FourServersScenario {
  self: KeysUtilities =>
  //4-servers scenario related stuff
  val serversIds: Seq[Key] = (1 to 4).toList.map("s" + _)
  val serversIdsAsSet: Set[QuorumSystemThresholdQuModel.Key] = serversIds.toSet
  val serversKeys: Map[ServerId, Map[ServerId, Key]] =
    serversIds.map(id => id -> keysForServer(id, serversIdsAsSet)).toMap
  val initialValue = 1
  val thresholds: QuorumSystemThresholds = QuorumSystemThresholds(t = 1, q = 3, b = 0)


}
