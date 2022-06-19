package qu.client.quorum

import io.grpc.{Status, StatusRuntimeException}
import qu.model.ConcreteQuModel.{OHS, Operation, ReplicaHistory, Request, Response, ServerId}
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{ExceptionsInspector, ResponsesGatherer}
import qu.ListUtils.{getMostFrequentElement, getMostFrequentElementWithOccurrences}
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

    def gatherResponsesAndOhs(): Future[(Seq[Response[Option[AnswerT]]], OHS)] = {
      for {
        responses <- mapThrowable(gatherResponses[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](
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

      getMostFrequentElementWithOccurrences(responses
        .map(response => (response.answer, {
          println("AAA la aswer: " + response.answer)
          val (rh, _) = response.authenticatedRh
          rh
        })))
        .map { e => {
          println("oooo, la e: " + e);
          val a = e match {
            case ((answer, _), order) => {
              System.exit(1)
              println("beccatooooo!!!")
              (answer, order)
            }
            case a => System.exit(1);println("ciao"); a
          }
          println("il case returns : " + a)
          e
        }
        }
        .map { case ((answer, _), order) => (answer, order) }
        .getOrElse(throw new Error("inconsistent client protocol state"))
    }

    for {
      (responses, ohs) <- gatherResponsesAndOhs()
      (answer, voteCount) <- scrutinize(responses)
    } yield (answer, voteCount, ohs)
  }


  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId,
    ResponseT]], exceptionsByServerId: MutableMap[ConcreteQuModel.ServerId, Throwable])
  : Unit = inspectExceptions(completionPromise, exceptionsByServerId, thresholds)
}