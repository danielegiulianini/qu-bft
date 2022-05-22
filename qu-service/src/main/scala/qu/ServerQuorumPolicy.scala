package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.Shared.{QuorumSystemThresholds, RecipientInfo}
import qu.protocol.model.ConcreteQuModel._
import qu.protocol.model.ConcreteQuModel.{LogicalTimestamp, OHS, Operation, Request, Response, ServerId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Success

trait ServerQuorumPolicy[Marshallable[_], ObjectT] {
  def objectSync[AnswerT](lt: LogicalTimestamp)(implicit
                                                marshallableRequest: Marshallable[LogicalTimestamp],
                                                marshallableResponse: Marshallable[ObjectSyncResponse[ObjectT, AnswerT]]
  ): Future[Option[(ObjectT, AnswerT)]]
}

class SimpleServerQuorumPolicy[Marshallable[_], ObjectT](servers: Map[String, GrpcClientStub[Marshallable]],
                                                         private val retryingTime: FiniteDuration = 3.seconds)
extends AbstractQuorumPolicy[Marshallable](servers, retryingTime) with ServerQuorumPolicy[Marshallable, ObjectT] {

  override def objectSync[AnswerT](lt: LogicalTimestamp)
                                  (implicit
                                   marshallableRequest: Marshallable[LogicalTimestamp],
                                   marshallableResponse: Marshallable[ObjectSyncResponse[ObjectT, AnswerT]]
                                  ): Future[Option[(ObjectT, AnswerT)]] = {
    gatherResponses[LogicalTimestamp, ObjectSyncResponse[ObjectT, AnswerT]](lt,
      responsesQuorum = 6,
      filterSuccess = response => response.responseCode == StatusCode.SUCCESS && !response.answer.isEmpty
    ).map(_.values.head.answer)
  }
}



object ServerQuorumPolicy {
  type ServerQuorumPolicyFactory[Marshallable[_], U] = (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Marshallable, U]

  //todo: this is only a stub
  def simpleJacksonServerQuorumFactory[U](): ServerQuorumPolicyFactory[JavaTypeable, U] =
    (mySet, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](servers = Map())

}





/* old code without refactoring:
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