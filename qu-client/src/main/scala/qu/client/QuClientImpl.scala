package qu.client


import qu.client.QuClient
import qu.client.backoff.BackOffPolicy
import qu.client.quorum.ClientQuorumPolicy
import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.ConcreteOperationTypes._
import qu.model.QuorumSystemThresholds

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}


class QuClientImpl[ObjectT, Transportable[_]](private var policy: ClientQuorumPolicy[ObjectT, Transportable] /* with Shutdownable*/ ,
                                              private var backoffPolicy: BackOffPolicy,
                                              private val serversIds: Set[String], //only servers ids are actually required in this class
                                              private val thresholds: QuorumSystemThresholds)
  extends QuClient[ObjectT, Transportable] {

  private val logger = Logger.getLogger(classOf[QuClientImpl[ObjectT, Transportable]].getName)

  private def logA(level: Level = Level.INFO, msg: String, param1: Int = 2)(implicit ec: ExecutionContext) = Future {
    logger.log(Level.WARNING, msg)
  }

  private def log(level: Level = Level.INFO, msg: String, param1: Int = 2) =
    logger.log(Level.WARNING, msg)

  var cachedOhs: OHS = emptyOhs(serversIds)

  //  var lastOperation: Future[_] = Future.unit
  override def submit[T](op: Operation[T, ObjectT])(implicit
                                                    ec: ExecutionContext,
                                                    transportableRequest: Transportable[Request[T, ObjectT]],
                                                    transportableResponse: Transportable[Response[Option[T]]],
                                                    transportableRequestObj: Transportable[Request[Object, ObjectT]],
                                                    transportableResponseObj: Transportable[Response[Option[Object]]]):
  Future[T] = {
    println("sumbitting inside QuClientImpl ");

    //var lastOperation: Future[_] = Future.unit
    def submitWithOhs(ohs: OHS): Future[T] = {
      for {
        (answer, order, updatedOhs) <- policy.quorum[T](Some(op), ohs)
        _ <- updateCachedOhs(updatedOhs)
        answer <- if (order < thresholds.q) for {
          (opType, _, _) <- classifyAsync(updatedOhs)
          optimisticAnswer <- if (opType == METHOD) for {
            _ <- logA(msg = "returning value of query executed optimistically (as type is METHOD but responses' order less than q (" + order + ").")
            optimisticAnswer <- Future(
              answer.getOrElse(
                throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a None answer")))
          } yield optimisticAnswer else for {
            _ <- logA(msg = "opType: " + opType + ", so repairing.")
            repairedOhs <- repair(updatedOhs)
            newAnswer <- submitWithOhs(repairedOhs)
            _ <- updateCachedOhs(updatedOhs)
          } yield newAnswer
        } yield optimisticAnswer else Future(answer.getOrElse(
          throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a None answer")))
      } yield answer
    }

    /*this.synchronized {
      lastOperation = lastOperation.map(_ => submitWithOhs(emptyOhs(serversIds)))
    }*/

    submitWithOhs(cachedOhs)
  }

  private def updateCachedOhs(updatedCachedOhs: OHS)(implicit executionContext: ExecutionContext): Future[Unit] = {
    Future {
      synchronized {
        cachedOhs = updatedCachedOhs
      }
    }
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
