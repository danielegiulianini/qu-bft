package qu.service.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.ConcreteQuModel.Key
import qu.model.QuorumSystemThresholds
import qu.service.QuServerBuilder

import scala.concurrent.ExecutionContext

object RemoteCounterServer {
  def builder(ip: String, port: Int, privateKey: Key, thresholds: QuorumSystemThresholds, initialValue :Int = 0)(implicit executor: ExecutionContext)
  : QuServerBuilder[JavaTypeable, Int] =
    QuServerBuilder.jacksonSimpleServerBuilder[Int](
      ip, port, privateKey,
      thresholds,
      initialValue).addOperationOutput[Int]().addOperationOutput[Unit]()
}
