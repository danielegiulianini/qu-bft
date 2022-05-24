package qu.client

//import that declares specific dependency

import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.{OneShotAsyncScheduler, Shutdownable}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AuthenticatedQuClientImpl[U, Transportable[_]](private var policy: ClientQuorumPolicy[U, Transportable] with Shutdownable,
                                                     private val serversIds: Set[String], //only servers ids is actually required in this class
                                                     private val thresholds: QuorumSystemThresholds,
                                                     private var initialBackOffTime: FiniteDuration = 1000.millis)
  extends QuClient[U, Transportable] {

  private val scheduler = new OneShotAsyncScheduler(1) //concurrency level configurable by user??

  import scala.concurrent.ExecutionContext.Implicits.global //todo temporneous

  private val ohs: OHS = emptyOhs(serversIds)

  override def submit[T](op: Operation[T, U])(implicit
                                              //ec: ExecutionContext,
                                              transportableRequest: Transportable[Request[T, U]],
                                              transportableResponse: Transportable[Response[Option[T]]],
                                              transportableRequestObj: Transportable[Request[Object, U]],
                                              transportableResponseObj: Transportable[Response[Option[Object]]]):
  Future[T] = {
    for {
      (answer, order, ohs) <- policy.quorum(Some(op), ohs)
      answer <- if (order < thresholds.q) for {
        _ <- repair(ohs) //this updates ohs
        newAnswer <- submit(op)
      } yield newAnswer else Future(answer.getOrElse(throw new RuntimeException("illegal protocol State exception..."))) //when using option: Future(answer.get)
    } yield answer
  }

  private def repair(ohs: OHS)(implicit
                               transportableRequest: Transportable[Request[Object, U]],
                               transportableResponse: Transportable[Response[Option[Object]]]): Future[OHS] = {

    //utilities
    // todo 1.can be a point of polymorphism!(could use functional object)
    def backOff(): Future[Void] = {
      initialBackOffTime *= 2
      scheduler.scheduleOnceAsPromise(initialBackOffTime)
    }

    def backOffAndRetry(): Future[OHS] = for {
      _ <- backOff()
      //perform a barrier or a copy
      (_, _, ohs) <- policy.quorum(Option.empty[Operation[Object, U]], ohs) //here Object is fundamental as server could return other than T
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType)
    } yield ohs

    def classifyAsync(ohs: OHS) = Future {
      classify(ohs, thresholds.r, thresholds.q)
    }

    def backOffAndRetryUntilMethod(operationType: ConcreteOperationTypes): Future[OHS] =
      if (operationType != ConcreteOperationTypes.METHOD) backOffAndRetry() else Future {
        ohs
      }

    //actual logic
    for {
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType)
    } yield ohs
  }

  //should shutdown policy...
  override def shutdown(): Unit = policy.shutdown()
}

object ProvaUserSide {

  //QuClientImpl(cluster, thresholds)

  //QuClientImpl(thresholds,
  //ips = Set(("www.google.com", port = 2), ("www.amazon.com", port=80))
  /*QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ips = Set("www.google.com", "www.amazon.com"))*/

  /*QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ipsWithKeyPaths = Set(("www.google.com", "pathForGoogle"), ("www.amazon.com", "pathForAmazon")))*/
}