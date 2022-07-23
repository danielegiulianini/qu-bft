package qu.service.quorum

import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{ExceptionsInspector, ResponsesGatherer}
import qu.stub.client.AbstractAsyncClientStub

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.mutable.{Map => MutableMap}


/**
 * Implements [[qu.service.quorum.ServerQuorumPolicy]] by keeping broadcasting to replicas the client request
 * until enough responses (b + 1, where b is the the count of byzantine services) are found.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param servers the client stubs by which to communicate to replicas.
 * @param retryingTime the interval after which a new broadcast is issued.
 * @tparam ObjectT type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
class BroadcastServerQuorumPolicy[Transportable[_], ObjectT](servers: Map[ServerId, AbstractAsyncClientStub[Transportable]],
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

  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId, ResponseT]],
                                                      exceptionsByServerId: MutableMap[ConcreteQuModel.ServerId, Throwable])
  : Unit = inspectExceptions(completionPromise, exceptionsByServerId, thresholds)
}
