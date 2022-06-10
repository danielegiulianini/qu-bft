package qu.service.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.ConcreteQuModel.Key
import qu.model.QuorumSystemThresholds
import qu.service.{QuServerBuilder}

object RemoteCounterServer {
  def builder(ip: String, port: Int, privateKey: Key, thresholds: QuorumSystemThresholds, initialValue :Int = 0)
  : QuServerBuilder[JavaTypeable, Int] =
    QuServerBuilder.jacksonSimpleServerBuilder[Int](
      ip, port, privateKey,
      thresholds,
      initialValue).addOperationOutput()[Int].addOperationOutput()[Unit]
}
