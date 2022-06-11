package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.{JwtGrpcClientStub, RecipientInfo, ResponsesGatherer, Shutdownable}
import qu.StubFactories.distributedJacksonJwtStubFactory
import qu.auth.Token
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}

//import that declares specific dependency
import qu.model.ConcreteQuModel._


trait ClientQuorumPolicy[ObjectT, Transportable[_]] extends Shutdownable {
  def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
                      ohs: OHS)
                     (implicit
                      transportableRequest: Transportable[Request[AnswerT, ObjectT]],
                      transportableResponse: Transportable[Response[Option[AnswerT]]]): Future[(Option[AnswerT], Int, OHS)]
}

//basic policy (maybe some logic could be shared by subclasses... in the case can be converted to trait)
class SimpleBroadcastClientPolicy[ObjectT, Transportable[_]](private val thresholds: QuorumSystemThresholds,
                                                             protected val servers: Map[ServerId, JwtGrpcClientStub[Transportable]],
                                                             private val retryingTime: FiniteDuration = 3.seconds)(implicit ec:ExecutionContext)
  extends ResponsesGatherer[Transportable](servers, retryingTime) with ClientQuorumPolicy[ObjectT, Transportable] {


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
        responses <- gatherResponses[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](
          request = Request[AnswerT, ObjectT](operation, ohs),
          responsesQuorum = thresholds.q,
          filterSuccess = _.responseCode == StatusCode.SUCCESS)
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
}


class JacksonSimpleBroadcastClientPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtGrpcClientStub[JavaTypeable]])(implicit ec:ExecutionContext)
  extends SimpleBroadcastClientPolicy[ObjectT, JavaTypeable](thresholds, servers)


object ClientQuorumPolicy {

  //policy factories
  type ClientPolicyFactory[Transportable[_], ObjectT] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable] with Shutdownable

  //without tls
  def simpleJacksonPolicyFactoryUnencrypted[ObjectT](jwtToken: Token)(implicit executionContext: ExecutionContext): ClientPolicyFactory[JavaTypeable, ObjectT] =
    (servers, thresholds) => new SimpleBroadcastClientPolicy(thresholds,
      servers.map { recipientInfo => id(recipientInfo) -> distributedJacksonJwtStubFactory(jwtToken, recipientInfo.ip, recipientInfo.port, executionContext) }.toMap)

  //with tls
  def simpleJacksonPolicyFactoryWithTls[U](jwtToken: String): ClientPolicyFactory[JavaTypeable, U] = ???
}


//another policy requiring object's preferredQuorum... (as type class)


/*var myOhs: OHS = ohs
var currentSuccessSet = successSet //new set need for preventing reassignment to val

val cancelable: Cancelable = scheduler.scheduleOnceAsCallback(retryingTime)(gatherResponsesAndOhs(completionPromise, successSet)) //passing all the servers  the first time

(servers -- successSet.keySet)
  .map { case (serverId, stubToServer) => (serverId,
    stubToServer.send2[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](toBeSent = Request(operation, ohs)))
  }
  .foreach { case (serverId, responseFuture) => responseFuture.onComplete({
    case Success(response) if response.responseCode == StatusCode.SUCCESS =>
      //mutex needed because of multithreaded ex context
      this.synchronized {
        myOhs = myOhs + (serverId -> response.authenticatedRh)
        currentSuccessSet = currentSuccessSet + (serverId -> response)
        if (currentSuccessSet.size == thresholds.q) {
          cancelable.cancel()
          completionPromise success ((successSet.values.toSet, myOhs))
        }
      }
    case _ => //can happen exception for which must inform client user? no need to do nothing, only waiting for other servers' responses
  })
  }

completionPromise.future
}*/


/* come era prima...
object ClientQuorumPolicy {

  //policy factories
  type PolicyFactory[Marshallable[_], U] = (Set[ServerInfo], QuorumSystemThresholds) => ClientQuorumPolicy[U, Marshallable]

  //without tls
  def simpleJacksonPolicyFactoryUnencrypted[U](jwtToken: String): PolicyFactory[JavaTypeable, U] =
    (servers, thresholds) => new SimpleBroadcastPolicyClient(thresholds,
      servers.map(serverInfo => serverInfo.ip -> distributedJacksonJwtStubFactory(jwtToken, serverInfo)).toMap)

  //with tls
  def simpleJacksonPolicyFactoryWithTls[U](jwtToken: String): PolicyFactory[JavaTypeable, U] = ???
}
 */