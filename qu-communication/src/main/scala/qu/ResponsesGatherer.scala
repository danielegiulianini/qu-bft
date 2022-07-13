package qu

import qu.model.ConcreteQuModel.ServerId
import qu.model.ValidationUtils
import qu.stub.client.AsyncClientStub

import java.util.logging.{Level, Logger}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.collection.mutable.{Map => MutableMap}


abstract class ResponsesGatherer[Transportable[_]](servers: Map[ServerId, AsyncClientStub[Transportable]],
                                                   private val retryingTime: FiniteDuration = 1.seconds)
                                                  (implicit ec: ExecutionContext)
  extends Shutdownable {

  //  ValidationUtils.requireNonNullAsInvalid(servers) breaks scalamock

  private val logger = Logger.getLogger(classOf[ResponsesGatherer[Transportable]].getName)

  private def log(level: Level = Level.WARNING, msg: String, param1: Int = 2): Unit =
    logger.log(level, msg)

  private val scheduler = new OneShotAsyncScheduler(1)

  def gatherResponses[RequestT, ResponseT](request: RequestT,
                                           responsesQuorum: Int,
                                           successResponseFilter: ResponseT => Boolean)(implicit transportableRequest: Transportable[RequestT],
                                                                                        transportableResponse: Transportable[ResponseT]):
  Future[Map[ServerId, ResponseT]] = {

    def gatherResponsesImpl(request: RequestT,
                            completionPromise: Promise[Map[ServerId, ResponseT]] = Promise[Map[ServerId, ResponseT]](),
                            responseSet: Map[ServerId, ResponseT],
                            exceptionsByServerId: MutableMap[ServerId, Throwable],
                            responsesQuorum: Int,
                            successResponseFilter: ResponseT => Boolean)(implicit transportableRequest: Transportable[RequestT],
                                                                         transportableResponse: Transportable[ResponseT]):
    Future[Map[ServerId, ResponseT]] = {
      log(msg = "broadcasting to: " + servers + ".")
      var currentResponseSet = responseSet

      //not requires lock here as 1. cancelable val is not shared at this moment and 2. scheduler scheduleOnceAsCallback being thread-safe
      val cancelable = scheduler.scheduleOnceAsCallback(retryingTime)({
        logger.log(Level.INFO, "timeout is over, some server responses missing (or unsuccessful), so re-broadcasting. ")
        gatherResponsesImpl(request,
          completionPromise,
          responseSet,
          exceptionsByServerId,
          responsesQuorum,
          successResponseFilter)
      })

      (servers -- responseSet.keySet)
        .map { case (serverId, stubToServer) => (serverId,
          stubToServer.send[RequestT, ResponseT](toBeSent = request))
        }
        .foreach { case (serverId, stubToServer) => stubToServer.onComplete({
          case Success(response) if successResponseFilter(response) =>
            log(msg = "received response: " + response)
            this.synchronized { //mutex needed because of possible multithreaded ex context (user provided)
              currentResponseSet = currentResponseSet + (serverId -> response)
              //if a request from the same server arrives 2 times, could complete 2 times if not checking
              if (currentResponseSet.size == responsesQuorum && !completionPromise.isCompleted) {
                log(msg = "quorum obtained, so completing promise.")
                cancelable.cancel()
                completionPromise success currentResponseSet
              }
            }
          case Failure(ex) =>
            log(msg = "received exception by " + serverId + ".")
            this.synchronized(
              inspectExceptions[ResponseT](completionPromise, {
                exceptionsByServerId.put(serverId, ex);
                exceptionsByServerId
              })
            )
          case _ => //ignored since not interested in this situation
        })
        }
      completionPromise.future
    }

    gatherResponsesImpl(request,
      completionPromise = Promise[Map[ServerId, ResponseT]](),
      responseSet = Map(),
      MutableMap[ServerId, Throwable](),
      responsesQuorum,
      successResponseFilter)
  }

  protected def inspectExceptions[ResponseT](completionPromise: Promise[Map[ServerId, ResponseT]],
                                             exceptionsByServerId: MutableMap[ServerId, Throwable]): Unit

  override def shutdown(): Future[Unit] = {
    Future.reduceLeft[Unit, Unit](servers.values.toList.map(_.shutdown())
    )((_, _) => ()).map(_ => scheduler.shutdown()) //Future.sequence(servers.values.map(s => s.shutdown()))
  }

  override def isShutdown: Boolean = servers.values.forall(_.isShutdown) && scheduler.isShutdown
}


trait Startable {
  def start(): Unit
}

trait Shutdownable {
  def shutdown(): Future[Unit]

  def isShutdown: Boolean
}
