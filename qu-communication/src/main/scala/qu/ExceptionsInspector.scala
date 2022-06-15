package qu

import qu.ListUtils.getMostFrequentElement
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}
import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.Promise
import scala.collection.mutable.{Map => MutableMap}

trait ExceptionsInspector[Transportable[_]] {
  self: ResponsesGatherer[Transportable] =>

  protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId, ResponseT]],
                                             exceptionsByServerId: MutableMap[ServerId, Throwable],
                                             thresholds: QuorumSystemThresholds): Unit = {
    println("received excpetion...")
    //se ricevo exception da più di t server allora c'è un problema serio(o problema settaggio client o almeno un server malevolo)
    println("exceptions received: " + exceptionsByServerId.size + ", t is " + thresholds.t)
    if (exceptionsByServerId.size > thresholds.t) {
      println("t+1 threshold exceptions reached")
      getMostFrequentElement(exceptionsByServerId.values).map(completionPromise.failure(_))
    }
  }
}
