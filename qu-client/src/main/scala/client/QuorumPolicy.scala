
import GrpcClientStub.UnauthenticatedJacksonClientStub
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannelBuilder
import Shared.{QuorumSystemThresholds, RecipientInfo => ServerInfo}
import StubFactoryContainer.distributedJacksonJwtStubFactory
import qu.protocol.model.ConcreteQuModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

//import that declares specific dependency
import ConcreteQuModel._


trait QuorumPolicy[U, Marshallable[_]] {
  def quorum[T](operation: Option[Operation[T, U]],
                ohs: OHS)
               (implicit
                marshallableRequest: Marshallable[Request[T, U]],
                marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Option[T], Int, OHS)]
}

object QuorumPolicy {
  //policy factories
  type PolicyFactory[Marshallable[_], U] = (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Marshallable]

  //without tls
  def simpleJacksonPolicyFactoryUnencrypted[U](jwtToken: String): PolicyFactory[JavaTypeable, U] =
    (mySet, thresholds) => new SimpleBroadcastPolicy(thresholds,
      mySet.map(serverInfo => serverInfo.ip -> distributedJacksonJwtStubFactory(jwtToken, serverInfo)).toMap)

  //with tls
  def simpleJacksonPolicyFactoryWithTls[U](jwtToken: String): PolicyFactory[JavaTypeable, U] = ???
}


//basic policy (maybe some logic could be shared by subclasses... in the case can be converted to trait)
class SimpleBroadcastPolicy[U, Marshallable[_]](private val thresholds: QuorumSystemThresholds,
                                                private val servers: Map[ServerId, JwtGrpcClientStub[Marshallable]],
                                                private val startingBackOffTime: FiniteDuration = 3.seconds)
  extends QuorumPolicy[U, Marshallable] {

  //is it possible to have overlapping calls to schedule? (only so it's convenient to use >1 threads)?? no, actually!
  private val scheduler = new OneShotAsyncScheduler(2) //concurrency level configurable by user??

  override def quorum[T](operation: Option[Operation[T, U]],
                         ohs: OHS)
                        (implicit
                         marshallableRequest: Marshallable[Request[T, U]],
                         marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Option[T], Int, OHS)] = {
    def gatherResponses(completionPromise: Promise[(Set[Response[Option[T]]], OHS)],
                        successSet: Map[ServerId, Response[Option[T]]])
                       (implicit
                        marshallableRequest: Marshallable[Request[T, U]],
                        marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Set[Response[Option[T]]], OHS)] = {
      var myOhs: OHS = ohs
      //new set need for preventing reassignment to val
      var currentSuccessSet = successSet
      val cancelable = scheduler.scheduleOnceAsCallback(startingBackOffTime)(gatherResponses(completionPromise, successSet)) //passing all the servers  the first time

      (servers -- successSet.keySet)
        //without partial function:
        .map(kv => (kv._1,
          kv._2.send2[Request[T, U], Response[Option[T]]](toBeSent = Request(operation, ohs))))
        .foreach(kv => kv._2.onComplete({
          /*.map { case (serverId, stubToServer) => (serverId,
            stubToServer.send2[Request[T, U], Response[Option[T]]](toBeSent = Request(operation, ohs)))
          }*/
          /*.foreach{case (serverId, responseFuture) => responseFuture.onComplete({*/
          case Success(response) if response.responseCode == StatusCode.SUCCESS =>
            //mutex needed because of multithreaded ex context
            this.synchronized {
              myOhs = myOhs + (kv._1 -> response.authenticatedRh)
              currentSuccessSet = currentSuccessSet + ((kv._1, response))
              /*myOhs = myOhs + (serverId -> response.authenticatedRh)
              currentSuccessSet = currentSuccessSet + ((responseFuture, response))*/
              if (currentSuccessSet.size == thresholds.q) {
                cancelable.cancel()
                completionPromise success ((successSet.values.toSet, myOhs))
              }
            }
          case _ => //can happen exception for which must inform client user? no need to do nothing, only waiting for other servers' responses
        }))

      completionPromise.future
    }

    def scrutinize(responses: Set[Response[Option[T]]]): Future[(ConcreteQuModel.Response[Option[T]], Int)] = Future {
      responses
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .maxByOption(_._2)
        .getOrElse(throw new RuntimeException("inconsistent protocl state"))
    }

    //actual logic
    for {
      (responses, ohs) <- gatherResponses(Promise(), successSet = Map())
      (response, voteCount) <- scrutinize(responses)
    } yield (response.answer, voteCount, ohs)
  }
}