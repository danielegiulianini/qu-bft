package qu.model

import qu.model.ConcreteQuModel.{Key, ServerId}

//or as obj or as  a case class
trait FourServersScenario {
  self: KeysUtilities =>
  //4-servers scenario related stuff
  val serversIds = (1 to 4).toList.map("s" + _)
  val serversIdsAsSet = serversIds.toSet
  val serversKeys: Map[ServerId, Map[ServerId, Key]] =
    serversIds.map(id => id -> keysForServer(id, serversIdsAsSet)).toMap
  val initialValue = 1
  val thresholds = QuorumSystemThresholds(t = 1, q = 3, b = 0)


}
