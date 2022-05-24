package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.{AbstractQuorumPolicy, GrpcClientStub}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait ServerQuorumPolicy[Transportable[_], ObjectT] {
  def objectSync(lt: LogicalTimestamp)(implicit
                                                transportableRequest: Transportable[LogicalTimestamp],
                                                transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
  ): Future[ObjectT]
}

class SimpleServerQuorumPolicy[Transportable[_], ObjectT](servers: Map[String, GrpcClientStub[Transportable]],
                                                          private val thresholds: QuorumSystemThresholds,
                                                          private val retryingTime: FiniteDuration = 3.seconds)
extends AbstractQuorumPolicy[Transportable](servers, retryingTime) with ServerQuorumPolicy[Transportable, ObjectT] {

  override def objectSync(lt: LogicalTimestamp)
                                  (implicit
                                   transportableRequest: Transportable[LogicalTimestamp],
                                   transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
                                  ): Future[ObjectT] = {
    gatherResponses[LogicalTimestamp, ObjectSyncResponse[ObjectT]](lt,
      responsesQuorum = 6,//todo come ricavo b??
      filterSuccess = response => response.responseCode == StatusCode.SUCCESS && !response.answer.isEmpty
    ).map(_.values.head.answer.getOrElse(throw new Exception(" inconsistent...")))
  }
}



object ServerQuorumPolicy {
  type ServerQuorumPolicyFactory[Marshallable[_], U] = (Map[String, Int], QuorumSystemThresholds) => ServerQuorumPolicy[Marshallable, U]

  //todo: this is only a stub
  def simpleJacksonServerQuorumFactory[U](): ServerQuorumPolicyFactory[JavaTypeable, U] =
    (serversMap, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](servers = Map(), thresholds)

}





/* old code without refactoring:
    private val scheduler = new qu.OneShotAsyncScheduler(2) //concurrency level configurable by user??

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
        //new set need for preventing reassignment to val
        var currentSuccessSet = successSet
        val cancelable = scheduler.scheduleOnceAsCallback(retryingTime)(gatherResponses(completionPromise, successSet)) //passing all the servers  the first time

        (servers -- successSet.keySet)
          //without partial function:
          .map(kv => (kv._1,
            kv._2.send2[Request[T, U], Response[Option[T]]](toBeSent = lt)))
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
          .getOrElse(throw new Error("inconsistent client protocol state"))
      }

      //actual logic
      for {
        (responses, ohs) <- gatherResponses(Promise(), successSet = Map())
        (response, voteCount) <- scrutinize(responses)
      } yield (response.answer, voteCount, ohs)
    }*/