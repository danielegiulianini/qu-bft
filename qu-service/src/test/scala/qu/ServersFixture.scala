package qu

import qu.RecipientInfo.id
import qu.model.ConcreteQuModel.{Key, OHS, ServerId}
import qu.model.{OHSFixture, QuorumSystemThresholds}
import qu.service.AbstractQuService.ServerInfo

//"fixture-context objects" pattern from https://www.scalatest.org/user_guide/sharing_fixtures#fixtureContextObjects
//(as don't need to clean up after.)
trait ServersFixture {
  self: OHSFixture =>

  val quServer1 = RecipientInfo(ip = "ciao2", port = 1)
  val quServer2 = RecipientInfo(ip = "localhost", port = 2)
  val quServer3 = RecipientInfo(ip = "localhost", port = 3)
  val quServer4 = RecipientInfo(ip = "localhost", port = 4)

  val keysByServer: Map[ServerId, Map[ServerId, Key]] = Map(
    id(quServer1) -> Map(id(quServer1) -> "ks1s1",
      id(quServer2) -> "ks1s2",
      id(quServer3) -> "ks1s3",
      id(quServer4) -> "ks1s4"),
    id(quServer2) -> Map(id(quServer1) -> "ks2s1",
      id(quServer2) -> "ks2s2",
      id(quServer3) -> "ks2s3",
      id(quServer4) -> "ks2s4"),
    id(quServer3) -> Map(id(quServer1) -> "ks3s1",
      id(quServer2) -> "ks3s2",
      id(quServer3) -> "ks3s3",
      id(quServer4) -> "ks3s4"),
    id(quServer4) -> Map(id(quServer1) -> "ks4s1",
      id(quServer2) -> "ks4s2",
      id(quServer3) -> "ks4s3",
      id(quServer4) -> "ks4s4"))

  val serverIds = keysByServer.keys.toSet

  val quServer1WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer1)))
  val quServer2WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer2)))
  val quServer3WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer3)))
  val quServer4WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer4)))

  val InitialObject = 2022
  val FaultyServersCount = 1
  val MalevolentServersCount = 0

  val thresholds = QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount)
  val aOhsWithMethod: OHS = ohsWithMethodFor(keysByServer)
  val aOhsWithInlineMethod: OHS = ohsWithInlineMethodFor(keysByServer, thresholds.r)
  val aOhsWithInlineBarrier: OHS = ohsWithInlineBarrierFor(keysByServer, thresholds.r)
  val aOhsWithBarrier: OHS = ohsWithBarrierFor(keysByServer)
}
