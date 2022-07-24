package qu

import qu.ListUtils.getMostFrequentElement
import qu.model.{QuorumSystemThresholdQuModel, QuorumSystemThresholds}
import qu.model.QuorumSystemThresholdQuModel.ServerId

import scala.concurrent.Promise
import scala.collection.mutable.{Map => MutableMap}

trait ExceptionsInspector[Transportable[_]] {
  self: ResponsesGatherer[Transportable] =>

  protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[QuorumSystemThresholdQuModel.ServerId, ResponseT]],
                                             exceptionsByServerId: MutableMap[ServerId, Throwable],
                                             thresholds: QuorumSystemThresholds): Unit = {

    //if receiving exceptions from more than t servers (t is the upper bound for faulty servers) then there is a
    //serious problem (either errors of client setup or some malevolent server not expected at the time of either
    //client or server configuration.
    if (exceptionsByServerId.size > thresholds.t  && !completionPromise.isCompleted) {
      getMostFrequentElement(exceptionsByServerId.values).map(completionPromise.failure(_))
    }
  }
}
