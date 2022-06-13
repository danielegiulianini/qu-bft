package qu

import io.grpc.ManagedChannel
import qu.model.ConcreteQuModel.ServerId
import qu.model.ValidationUtils
import qu.stub.client.AsyncClientStub

import java.util.concurrent.{ConcurrentHashMap => ThreadsafeMap}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait Startable {
  def start(): Unit
}

trait Shutdownable {
  def shutdown(): Future[Unit]

  def isShutdown: Boolean
}

abstract class ResponsesGatherer[Transportable[_]](servers: Map[String, AsyncClientStub[Transportable]],
                                                   private val retryingTime: FiniteDuration = 1.seconds)
                                                  (implicit ec: ExecutionContext)
  extends Shutdownable {

  ValidationUtils.requireNonNullAsInvalid(servers)

  private val scheduler = new OneShotAsyncScheduler(1)

  def gatherResponses[RequestT, ResponseT](request: RequestT,
                                           completionPromise: Promise[Map[ServerId, ResponseT]] = Promise[Map[ServerId, ResponseT]](),
                                           responseSet: Map[ServerId, ResponseT] = Map[ServerId, ResponseT](),
                                           responsesQuorum: Int,
                                           successResponseFilter: ResponseT => Boolean)(implicit transportableRequest: Transportable[RequestT],
                                                                                        transportableResponse: Transportable[ResponseT]):
  Future[Map[ServerId, ResponseT]] = {

    var currentResponseSet = responseSet
    val exceptionsByServerId: Map[ServerId, Throwable] = Map()

    //not requires lock here as it is not shared at this moment
    val cancelable = scheduler.scheduleOnceAsCallback(retryingTime)(gatherResponses(request,
      completionPromise,
      responseSet,
      responsesQuorum,
      successResponseFilter)) //passing all the servers the first time

    (servers -- responseSet.keySet)
      .map { case (serverId, stubToServer) => (serverId,
        stubToServer.send[RequestT, ResponseT](toBeSent = request))
      }
      .foreach { case (serverId, stubToServer) => stubToServer.onComplete({
        case Success(response) if successResponseFilter(response) =>
          this.synchronized { //mutex needed because of multithreaded ex context
            currentResponseSet = currentResponseSet + (serverId -> response)
            if (currentResponseSet.size == responsesQuorum) {
              cancelable.cancel()
              completionPromise success currentResponseSet
            }
          }
        case Failure(ex) => this.synchronized(
          inspectExceptions[ResponseT](completionPromise, exceptionsByServerId + (serverId -> ex))
        )
      })
      }
    completionPromise.future
  }

  protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ServerId, ResponseT]],
                                             exceptionsByServerId: Map[ServerId, Throwable]): Unit

  override def shutdown(): Future[Unit] =
    Future.reduce(servers.values.map(s => s.shutdown()))((_, _) => ()) //Future.sequence(servers.values.map(s => s.shutdown())) //servers.values.foreach(_.shutdown())

  override def isShutdown: Boolean = servers.values.forall(_.isShutdown)
}

