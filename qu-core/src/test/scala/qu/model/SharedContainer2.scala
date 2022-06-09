package qu.model

import qu.model.ConcreteQuModel.{ServerId, classify, setup}

//import scala.language.postfixOps

object SharedContainer {
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
}
/*
object ProvaApp extends App with OHSFixture {

  //todo can be shared with quClientSpec in ascenario case class
  val exampleServersIds = (1 to 4).toList.map("s" + _)
  val exampleServersKeys: Map[ServerId, Map[ConcreteQuModel.ServerId, ServerId]] =
    exampleServersIds.map(id => id -> keysForServer(id, exampleServersIds.toSet)).toMap
  val r = 2
  val q = 4
  println("la ohs con inline method *******************************")
  println(ohsWithInlineMethodFor(exampleServersKeys, r))
  println("++++++++++++++la classify lo sclassifica come: " + classify(ohsWithInlineMethodFor(exampleServersKeys, r), r, q)._1)
val operation = Some(IncrementAsObj)
  val clientId =Some( "ciao")
  setup(operation, ohsWithInlineMethodFor(exampleServersKeys, r), q, r, clientId.get)
}*/
