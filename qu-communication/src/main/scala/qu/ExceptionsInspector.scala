package qu

import qu.ListUtils.getMostFrequentElement
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}
import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.Promise

trait ExceptionsInspector[Transportable[_]] {
  self: ResponsesGatherer[Transportable] =>

  protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId, ResponseT]],
                                             exceptionsByServerId: Map[ServerId, Throwable],
                                             thresholds: QuorumSystemThresholds): Unit = {
    //se ricevo exception da più di t server allora c'è un problema serio(o problema settaggio client o almeno un server malevolo)
    if (exceptionsByServerId.size > thresholds.t) {
      getMostFrequentElement(exceptionsByServerId.values).map(completionPromise.failure(_))
    }
  }
}
