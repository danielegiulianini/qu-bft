package qu.client

//import that declares specific dependency

import qu.client.backoff.BackOffPolicy
import qu.client.quorum.ClientQuorumPolicy
import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.ConcreteOperationTypes._
import qu.model.QuorumSystemThresholds
import qu.{OneShotAsyncScheduler, Shutdownable}

import java.util.logging.Logger
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class AuthenticatedQuClientImpl[ObjectT, Transportable[_]](private var policy: ClientQuorumPolicy[ObjectT, Transportable] /* with Shutdownable*/ ,
                                                           private var backoffPolicy: BackOffPolicy,
                                                           private val serversIds: Set[String], //only servers ids are actually required in this class
                                                           private val thresholds: QuorumSystemThresholds)
  extends QuClient[ObjectT, Transportable] {
  private val logger = Logger.getLogger(classOf[AuthenticatedQuClientImpl[ObjectT, Transportable]].getName)

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
        answer <- if (order < thresholds.q) for {
          (opType, _, _) <- classifyAsync(updatedOhs)
          optimisticAnswer <- if (opType == METHOD) Future(
            answer.getOrElse(
              throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a None answer")))
          else for {
            repairedOhs <- repair(updatedOhs)
            newAnswer <- submitWithOhs(repairedOhs)
          } yield newAnswer
        } yield optimisticAnswer else Future(answer.getOrElse(
          throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a None answer")))
      } yield answer
      /*
            for {
              (answer, order, updatedOhs) <- policy.quorum(Some(op), ohs)
              (opType, _, _) <- classifyAsync(ohs)
              oanswer <- if (opType == METHOD) Future {
                answer
              } else for { a <- Future {
                answer
              }} yield a
            } yield oanswer.get*/
    }

    submitWithOhs(emptyOhs(serversIds))
  }

  private def classifyAsync(ohs: OHS) = Future.successful {
    classify(ohs, thresholds.r, thresholds.q)
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


    def backOffAndRetryUntilMethod(operationType: ConcreteOperationTypes, ohs: OHS): Future[OHS] =
      if (operationType != METHOD) backOffAndRetry() else Future {
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

  override def shutdown(): Future[Unit] = policy.shutdown()

  override def isShutdown: Flag = policy.isShutdown
}

