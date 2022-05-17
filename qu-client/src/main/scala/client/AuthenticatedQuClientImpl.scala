package client

//import that declares specific dependency

import qu.protocol.model.ConcreteQuModel._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class AuthenticatedQuClientImpl[U, Transferable[_]](private var policy: QuorumPolicy[U, Transferable],
                                                    private val thresholds: QuorumSystemThresholds)
  extends QuClient[U, Transferable] {

  private val scheduler = new OneShotAsyncScheduler(1) //concurrency level configurable by user??

  import scala.concurrent.ExecutionContext.Implicits.global  //temporneous

  //todo (after fixing nul authenticator)
  private val ohs: OHS = null //emptyOhs(

  override def submit[T](op: Operation[T, U])(implicit
                                              marshallableRequest: Transferable[Request[T, U]],
                                              marshallableResponse: Transferable[Response[Option[T]]],
                                              marshallableRequestObj: Transferable[Request[Object, U]],
                                              marshallableResponseObj: Transferable[Response[Option[Object]]]): Future[T] = {
    for {
      (answer, order, ohs) <- policy.quorum(Some(op), ohs)
      answer <- if (order < thresholds.q) for {
        _ <- repair(ohs) //this updates ohs
        newAnswer <- submit(op)
      } yield newAnswer else Future(answer.getOrElse(throw new RuntimeException("illegal protocol State exception..."))) //when using option: Future(answer.get)
    } yield answer
  }


  def repair(ohs: OHS)(implicit
                       marshallableRequest: Transferable[Request[Object, U]],
                       marshallableResponse: Transferable[Response[Option[Object]]]): Future[OHS] = {
    //utilities todo 1.can be a point of polymorphism! 2.
    def backOff(): Future[Void] = scheduler.scheduleOnceAsPromise(3.seconds)

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

    def backOffAndRetryUntilMethod(operationType: OperationType1): Future[OHS] =
      if (operationType != OperationType1.METHOD) backOffAndRetry() else Future {
        ohs
      }

    //actual logic
    for {
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType)
    } yield ohs
  }
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