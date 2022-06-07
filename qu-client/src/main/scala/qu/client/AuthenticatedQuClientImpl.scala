package qu.client

//import that declares specific dependency

import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds
import qu.{OneShotAsyncScheduler, Shutdownable}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AuthenticatedQuClientImpl[ObjectT, Transportable[_]](private var policy: ClientQuorumPolicy[ObjectT, Transportable] /* with Shutdownable*/ ,
                                                           private var backoffPolicy: BackOffPolicy,
                                                           private val serversIds: Set[String], //only servers ids are actually required in this class
                                                           private val thresholds: QuorumSystemThresholds)
  extends QuClient[ObjectT, Transportable] {

  override def submit[T](op: Operation[T, ObjectT])(implicit
                                                    ec: ExecutionContext,
                                                    transportableRequest: Transportable[Request[T, ObjectT]],
                                                    transportableResponse: Transportable[Response[Option[T]]],
                                                    transportableRequestObj: Transportable[Request[Object, ObjectT]],
                                                    transportableResponseObj: Transportable[Response[Option[Object]]]):
  Future[T] = {
    def submitWithOhs(ohs: OHS): Future[T] = {
      for {
        (answer, order, updatedOhs) <- policy.quorum(Some(op), ohs)
        //todo mapping grpc exceptions to custom
        //todo optimistic query execution
        answer <- if (order < thresholds.q) for {
          repairedOhs <- repair(updatedOhs) //todo this updates ohs must here it's ignored!
          newAnswer <- submitWithOhs(repairedOhs)
        } yield newAnswer else Future(answer.getOrElse(
          throw new RuntimeException("illegal protocol State exception..."))
        )
      } yield answer
    }

    submitWithOhs(emptyOhs(serversIds))
  }

  private def repair(ohs: OHS)(implicit executionContext: ExecutionContext,
                               transportableRequest: Transportable[Request[Object, ObjectT]],
                               transportableResponse: Transportable[Response[Option[Object]]]): Future[OHS] = {
    //utilities
    def backOffAndRetry(): Future[OHS] = for {
      _ <- backoffPolicy.backOff()
      _ <- Future {
        println("after backing off")
      }
      //perform a barrier or a copy
      (_, _, ohs) <- policy.quorum(Option.empty[Operation[Object, ObjectT]],
        ohs) //here Object is fundamental as server could return other than T (could use Any too??)
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType, ohs)
    } yield ohs


    def classifyAsync(ohs: OHS) = Future {
      classify(ohs, thresholds.r, thresholds.q)
    }

    def backOffAndRetryUntilMethod(operationType: ConcreteOperationTypes, ohs: OHS): Future[OHS] =
      if (operationType != ConcreteOperationTypes.METHOD) backOffAndRetry() else Future {
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

  override def shutdown(): Unit = policy.shutdown()
}

