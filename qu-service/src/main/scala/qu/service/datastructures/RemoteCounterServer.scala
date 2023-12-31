package qu.service.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.model.QuorumSystemThresholdQuModel.Key
import qu.model.QuorumSystemThresholds
import qu.service.{QuServer, QuServerBuilder}

import scala.concurrent.ExecutionContext

object RemoteCounterServer {
  def builder(ip: String, port: Int, privateKey: Key, thresholds: QuorumSystemThresholds, initialValue: Int = 0)
             (implicit executor: ExecutionContext)
  : QuServerBuilder[JavaTypeable, Int] =
    builder(SocketAddress(ip, port), privateKey,
      thresholds,
      initialValue).addOperationOutput[Int]().addOperationOutput[Unit]()

  def builder(ri: SocketAddress, privateKey: Key, thresholds: QuorumSystemThresholds, initialValue: Int)
             (implicit executor: ExecutionContext)
  : QuServerBuilder[JavaTypeable, Int] =
    QuServer.builder[Int](
      ri.ip, ri.port, privateKey,
      thresholds,
      initialValue).addOperationOutput[Int]().addOperationOutput[Unit]()
}
