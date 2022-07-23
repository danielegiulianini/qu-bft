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

//basic policy (maybe some logic could be shared by subclasses... in the case can be converted to trait)

/**
 * Implements [[qu.client.quorum.ClientQuorumPolicy]] by keeping broadcasting to replicas the client request
 * until a quorum of responses is found.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param servers the replicas info.
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

  private val logger = Logger.getLogger(classOf[BroadcastClientQuorumPolicy[ObjectT, Transportable]].getName)

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
        .map { e => {
          println("oooo, la e: " + e);
          val a = e /*match {
            case ((answer2, _), order) => {
              System.exit(1)
              println("beccatooooo!!!")
              (answer2, order)
            }
            case a => System.exit(1);println("ciao");* a
          }*/
          println("il case returns : " + a)
          e
        }
        }
        .map(kv => (kv._1._1, kv._2)) //case ((answer, _), order) => (answer, order) }
        //.map { case ((answer, _), order) => (answer, order) }
        .getOrElse(throw new Error("inconsistent client protocol state"))
      //.getOrE
    }

    for {
      (responses, ohs) <- gatherResponsesAndOhs()
      (answer, voteCount) <- scrutinize(responses)
      _ <- Future.successful(println("la answer is: " + answer + "il votecount: " + voteCount))
    } yield (answer, voteCount, ohs)
  }


  override protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ConcreteQuModel.ServerId,
    ResponseT]], exceptionsByServerId: MutableMap[ConcreteQuModel.ServerId, Throwable])
  : Unit = inspectExceptions[ResponseT](completionPromise, exceptionsByServerId, thresholds)
}