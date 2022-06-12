package qu.service.quorum

import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, ServerId}
import qu.model.{QuorumSystemThresholds, StatusCode}
import qu.{AsyncGrpcClientStub, ResponsesGatherer}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class SimpleServerQuorumPolicy[Transportable[_], ObjectT](servers: Map[ServerId, AsyncGrpcClientStub[Transportable]],
                                                          private val thresholds: QuorumSystemThresholds,
                                                          private val retryingTime: FiniteDuration = 3.seconds)(implicit executor: ExecutionContext)
  extends ResponsesGatherer[Transportable](servers, retryingTime)
    with ServerQuorumPolicy[Transportable, ObjectT] {

  override def objectSync(lt: LogicalTimestamp)
                         (implicit
                          transportableRequest: Transportable[LogicalTimestamp],
                          transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
                         ): Future[ObjectT] = {
    gatherResponses[LogicalTimestamp, ObjectSyncResponse[ObjectT]](lt,
      responsesQuorum = thresholds.b,
      filterSuccess = response => response.responseCode == StatusCode.SUCCESS && response.answer.isDefined
    ).map(_.values.head.answer.getOrElse(throw new Exception(" inconsistent...")))
  }
}
