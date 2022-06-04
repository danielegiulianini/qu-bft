package qu.client

//import that declares specific dependency

import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.{OneShotAsyncScheduler, Shutdownable}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AuthenticatedQuClientImpl[U, Transportable[_]](private var policy: ClientQuorumPolicy[U, Transportable] /* with Shutdownable*/ ,
                                                     private var backoffPolicy: BackOffPolicy,
                                                     private val serversIds: Set[String], //only servers ids are actually required in this class
                                                     private val thresholds: QuorumSystemThresholds)
  extends QuClient[U, Transportable] {

  override def submit[T](op: Operation[T, U])(implicit
                                              ec: ExecutionContext,
                                              transportableRequest: Transportable[Request[T, U]],
                                              transportableResponse: Transportable[Response[Option[T]]],
                                              transportableRequestObj: Transportable[Request[Object, U]],
                                              transportableResponseObj: Transportable[Response[Option[Object]]]):
  Future[T] = {
    def submitWithOhs(ohs: OHS): Future[T] = {
      for {
        (answer, order, updatedOhs) <- policy.quorum(Some(op), ohs)
        //todo mapping exceptions?
        //todo optimistic query execution
        answer <- if (order < thresholds.q) for {
          repairedOhs <- repair(updatedOhs) //todo this updates ohs must here it's ignored!
          newAnswer <- submitWithOhs(repairedOhs)
        } yield newAnswer else Future(answer.getOrElse(throw new RuntimeException("illegal protocol State exception..."))) //when using option: Future(answer.get)
      } yield answer
    }

    submitWithOhs(emptyOhs(serversIds))
  }

  private def repair(ohs: OHS)(implicit executionContext: ExecutionContext,
                               transportableRequest: Transportable[Request[Object, U]],
                               transportableResponse: Transportable[Response[Option[Object]]]): Future[OHS] = {
    //utilities
    def backOffAndRetry(): Future[OHS] = for {
      _ <- backoffPolicy.backOff()
      _ <- Future {
        println("after backing off")
      }
      //perform a barrier or a copy
      (_, _, ohs) <- policy.quorum(Option.empty[Operation[Object, U]], ohs) //here Object is fundamental as server could return other than T
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType,ohs)
    } yield ohs


    def classifyAsync(ohs: OHS) = Future {
      classify(ohs, thresholds.r, thresholds.q)
    }

    def backOffAndRetryUntilMethod(operationType: ConcreteOperationTypes, ohs:OHS): Future[OHS] =
      if (operationType != ConcreteOperationTypes.METHOD) backOffAndRetry() else Future {
        println("la ohs che metto in future is:  " + ohs)
        ohs
      }

    //actual logic
    for {
      _ <- Future {
        println("so, starting to repair...")
      }
      (operationType, _, _) <- classifyAsync(ohs) //todo could use futureSuccessful here
      _ <- Future {
        println("after classifying, resulting type is: " + operationType)
      }
      ohs <- backOffAndRetryUntilMethod(operationType, ohs)
    } yield ohs
  }

  //todo
  override def shutdown(): Unit = {} //policy.shutdown()
}

