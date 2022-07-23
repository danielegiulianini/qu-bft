package qu.client.quorum

import io.grpc.{Status, StatusRuntimeException}
import qu.model.ConcreteQuModel.{OHS, Operation, ReplicaHistory, Request, Response, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{ExceptionsInspector, ResponsesGatherer}
import qu.ListUtils.{getMostFrequentElement, getMostFrequentElementWithOccurrences}
import qu.auth.common.FutureUtilities.mapThrowable
import qu.client.{OperationOutputNotRegisteredException, QuClientImpl}
import qu.stub.client.JwtAsyncClientStub

import java.util.logging.Logger
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.mutable.{Map => MutableMap}

/**
 * Implements [[qu.client.quorum.ClientQuorumPolicy]] by keeping broadcasting to replicas the client request
 * until a quorum of responses is found.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param servers the client stubs by which to communicate to replicas.
 * @param retryingTime the interval after which a new broadcast is issued.
 * @tparam ObjectT type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
class BroadcastClientQuorumPolicy[ObjectT, Transportable[_]](private val thresholds: QuorumSystemThresholds,
                                                             protected val servers: Map[ServerId,
                                                               JwtAsyncClientStub[Transportable]],
                                                             private val retryingTime: FiniteDuration = 3.seconds)
                                                            (implicit ec: ExecutionContext)
  extends ResponsesGatherer[Transportable](servers, retryingTime)
    with ClientQuorumPolicy[ObjectT, Transportable]
    with ExceptionsInspector[Transportable] {

  override def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
                               ohs: OHS)
                              (implicit
                               transportableRequest: Transportable[Request[AnswerT, ObjectT]],
                               transportableResponse: Transportable[Response[Option[AnswerT]]])
  : Future[(Option[AnswerT], Int, OHS)] = {

    def extractOhsFromResponses(responses: Map[ServerId, Response[Option[AnswerT]]]): OHS =
      responses.view.mapValues(_.authenticatedRh).toMap

    def gatherResponsesAndOhs(): Future[(Seq[Response[Option[AnswerT]]], OHS)] = {
      for {
        responses <- mapThrowable[Map[ServerId, Response[Option[AnswerT]]]](
          gatherResponses[Request[AnswerT, ObjectT],
            Response[Option[AnswerT]]](
            request = Request[AnswerT, ObjectT](operation, ohs),
            responsesQuorum = thresholds.q,
            successResponseFilter = _.responseCode == StatusCode.SUCCESS), {
            //mapping exception to more readable one
            case ex: StatusRuntimeException if ex.getStatus.getCode == Status.UNIMPLEMENTED.getCode =>
              OperationOutputNotRegisteredException()
            case thr => thr
          })
      } yield (responses.values.toSeq, extractOhsFromResponses(responses))
    }

    def scrutinize(responses: Seq[Response[Option[AnswerT]]])
    : Future[(Option[AnswerT], Int)] = Future {

      getMostFrequentElementWithOccurrences[(Option[AnswerT], ReplicaHistory)](responses
        .map(response => (response.answer, {
          val (rh, _) = response.authenticatedRh
          rh
        })))
        .map(kv => (kv._1._1, kv._2)) //more readable:         //.map { case ((answer, _), order) => (answer, order) }
        .getOrElse(throw new Error("inconsistent client protocol state"))
    }

    for {
      (responses, ohs) <- gatherResponsesAndOhs()
      (answer, voteCount) <- scrutinize(responses)
    } yield (answer, voteCount, ohs)
  }


  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId,
    ResponseT]], exceptionsByServerId: MutableMap[ConcreteQuModel.ServerId, Throwable])
  : Unit = inspectExceptions[ResponseT](completionPromise, exceptionsByServerId, thresholds)
}