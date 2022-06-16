package qu.client.quorum

import io.grpc.{Status, StatusRuntimeException}
import qu.model.ConcreteQuModel.{OHS, Operation, Request, Response, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{ExceptionsInspector, ResponsesGatherer}
import qu.ListUtils.getMostFrequentElement
import qu.auth.common.FutureUtilities.mapThrowable
import qu.client.OperationOutputNotRegisteredException
import qu.stub.client.JwtAsyncClientStub

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.mutable.{Map => MutableMap}

//basic policy (maybe some logic could be shared by subclasses... in the case can be converted to trait)
class SimpleBroadcastClientPolicy[ObjectT, Transportable[_]](private val thresholds: QuorumSystemThresholds,
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

    def gatherResponsesAndOhs(): Future[(Set[Response[Option[AnswerT]]], OHS)] = {
      for {
        responses <- mapThrowable(gatherResponses[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](
          request = Request[AnswerT, ObjectT](operation, ohs),
          responsesQuorum = thresholds.q,
          successResponseFilter = _.responseCode == StatusCode.SUCCESS), {
              //mapping exception to more readable one
          case ex: StatusRuntimeException if ex.getStatus.getCode == Status.UNIMPLEMENTED.getCode => OperationOutputNotRegisteredException()
        })
      } yield (responses.values.toSet, extractOhsFromResponses(responses))
    }

    def scrutinize(responses: Set[Response[Option[AnswerT]]])
    : Future[(ConcreteQuModel.Response[Option[AnswerT]], Int)] = Future {
      responses
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .maxByOption(_._2)
        .getOrElse(throw new Error("inconsistent client protocol state"))
    }

    //actual logic
    for {
      (responses, ohs) <- gatherResponsesAndOhs()
      (response, voteCount) <- scrutinize(responses)
    } yield (response.answer, voteCount, ohs)
  }

  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId,
    ResponseT]], exceptionsByServerId: MutableMap[ConcreteQuModel.ServerId, Throwable])
  : Unit = inspectExceptions(completionPromise, exceptionsByServerId, thresholds)
}
