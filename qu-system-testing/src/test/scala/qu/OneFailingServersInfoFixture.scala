package qu

import qu.SocketAddress.id
import qu.model.ConcreteQuModel.{Key, OHS, ServerId}
import qu.model.{ConcreteQuModel, OHSUtilities, QuorumSystemThresholds}
import qu.service.AbstractQuService.ServerInfo
import qu.service.AbstractServersFixture

trait OneFailingServersInfoFixture extends AbstractServersFixture {

  self: OHSUtilities =>

  val quServer1: SocketAddress = SocketAddress(ip = "localhost", port = 1010)
  val quServer2: SocketAddress = SocketAddress(ip = "localhost", port = 1011)
  val quServer3: SocketAddress = SocketAddress(ip = "localhost", port = 1012)
  val quServer4: SocketAddress = SocketAddress(ip = "localhost", port = 1013)

  var quServerIpPorts: Set[SocketAddress] = Set[SocketAddress]()
  quServerIpPorts = quServerIpPorts + quServer1
  quServerIpPorts = quServerIpPorts + quServer2
  quServerIpPorts = quServerIpPorts + quServer3
  quServerIpPorts = quServerIpPorts + quServer4

  val authServerInfo: SocketAddress = SocketAddress(ip = "localhost", port = 1016)

  val keysByServer: Map[ServerId, Map[ServerId, Key]] = Map(
    id(quServer1) -> Map(id(quServer1) -> "ks1s1",
      id(quServer2) -> "ks1s2",
      id(quServer3) -> "ks1s3",
      id(quServer4) -> "ks1s4"),
    id(quServer2) -> Map(id(quServer1) -> "ks1s2",
      id(quServer2) -> "ks2s2",
      id(quServer3) -> "ks2s3",
      id(quServer4) -> "ks2s4"),
    id(quServer3) -> Map(id(quServer1) -> "ks1s3",
      id(quServer2) -> "ks2s3",
      id(quServer3) -> "ks3s3",
      id(quServer4) -> "ks3s4"),
    id(quServer4) -> Map(id(quServer1) -> "ks1s4",
      id(quServer2) -> "ks2s4",
      id(quServer3) -> "ks3s4",
      id(quServer4) -> "ks4s4"))

  val serverIds: Set[ConcreteQuModel.ServerId] = keysByServer.keys.toSet

  val quServer1WithKey: ServerInfo = ServerInfo(ip = quServer1.ip,
    port = quServer1.port,
    keySharedWithMe = keysByServer(id(quServer1))(id(quServer1)))
  val quServer2WithKey: ServerInfo = ServerInfo(ip = quServer2.ip, port = quServer2.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer2)))
  val quServer3WithKey: ServerInfo = ServerInfo(ip = quServer3.ip, port = quServer3.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer3)))
  val quServer4WithKey: ServerInfo = ServerInfo(ip = quServer4.ip, port = quServer4.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer4)))

  var quServersInfo: Set[ServerInfo] = Set[ServerInfo]()
  quServersInfo = quServersInfo + quServer1WithKey
  quServersInfo = quServersInfo + quServer2WithKey
  quServersInfo = quServersInfo + quServer3WithKey
  quServersInfo = quServersInfo + quServer4WithKey

  val FaultyServersCount = 1
  val MalevolentServersCount = 0
  val thresholds: QuorumSystemThresholds = QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount)

  //ohs
  val aOhsWithMethod: OHS = ohsWithMethodFor(keysByServer)
  val aOhsWithInlineMethod: OHS = ohsWithInlineMethodFor(keysByServer, thresholds.r)
  val aOhsWithInlineBarrier: OHS = ohsWithInlineBarrierFor(keysByServer, thresholds.r)
  val aOhsWithBarrier: OHS = ohsWithBarrierFor(keysByServer)
  val aOhsWithCopy: OHS = ohsWithCopyFor(serverKeys = keysByServer)

  val InitialObject = 2022
}
