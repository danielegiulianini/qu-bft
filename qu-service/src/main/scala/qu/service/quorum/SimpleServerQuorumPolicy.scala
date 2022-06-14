package qu.service.quorum

import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{ExceptionsInspector, ResponsesGatherer}
import qu.stub.client.AsyncClientStub

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}

class SimpleServerQuorumPolicy[Transportable[_], ObjectT](servers: Map[ServerId, AsyncClientStub[Transportable]],
                                                          private val thresholds: QuorumSystemThresholds,
                                                          private val retryingTime: FiniteDuration = 3.seconds)
                                                         (implicit executor: ExecutionContext)
  extends ResponsesGatherer[Transportable](servers, retryingTime)
    with ServerQuorumPolicy[Transportable, ObjectT] with ExceptionsInspector[Transportable] {

  override def objectSync(lt: LogicalTimestamp)
                         (implicit
                          transportableRequest: Transportable[LogicalTimestamp],
                          transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
                         ): Future[ObjectT] = {
    gatherResponses[LogicalTimestamp, ObjectSyncResponse[ObjectT]](lt,
      responsesQuorum = thresholds.b,
      successResponseFilter = response => response.responseCode == StatusCode.SUCCESS && response.answer.isDefined
    ).map(_.values.head.answer.getOrElse(throw new Exception(" inconsistent...")))
  }

  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId, ResponseT]], exceptionsByServerId: Map[ConcreteQuModel.ServerId, Throwable]): Unit = inspectExceptions(completionPromise, exceptionsByServerId, thresholds)
}
