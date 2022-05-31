package qu

import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Success


trait Shutdownable {
  def shutdown(): Unit
}

abstract class AbstractQuorumPolicy[Transportable[_]](servers: Map[String, GrpcClientStub[Transportable]],
                                                      private val retryingTime: FiniteDuration = 3.seconds) extends Shutdownable{
  private val scheduler = new OneShotAsyncScheduler(2) //concurrency level configurable by user??
  //is it possible to have overlapping calls to schedule? (only so it's convenient to use >1 threads)?? no, actually!

  protected def gatherResponses[RequestT, ResponseT](request: RequestT,
                                                     completionPromise: Promise[Map[ServerId, ResponseT]] = Promise[Map[ServerId, ResponseT]](),
                                                     responseSet: Map[ServerId, ResponseT] = Map[ServerId, ResponseT](),
                                                     responsesQuorum: Int,
                                                     filterSuccess: ResponseT => Boolean)
                                                    (implicit transportableRequest: Transportable[RequestT],
                                                     transportableResponse: Transportable[ResponseT]
                                                    ): Future[Map[ServerId, ResponseT]] = {
    var currentResponseSet = responseSet
    val cancelable = scheduler.scheduleOnceAsCallback(retryingTime)(gatherResponses(request, completionPromise, responseSet, responsesQuorum, filterSuccess)) //passing all the servers  the first time

    (servers -- responseSet.keySet)
      .map { case (serverId, stubToServer) => (serverId,
        stubToServer.send[RequestT, ResponseT](toBeSent = request))
      }
      .foreach { case (serverId, stubToServer) => stubToServer.onComplete({
        case Success(response) if filterSuccess(response) =>
          this.synchronized { //mutex needed because of multithreaded ex context
            currentResponseSet = currentResponseSet + (serverId -> response)
            //todo se non tutti sono uguali lancio exception
            if (currentResponseSet.size == responsesQuorum) {
              cancelable.cancel()
              completionPromise success currentResponseSet
            }
          }
        case _ => //can happen exception for which must inform client user? no need to do nothing, only waiting for other servers' responses
      })
      }
    completionPromise.future
  }

  override def shutdown(): Unit = servers.values.foreach(_.shutdown())
}

