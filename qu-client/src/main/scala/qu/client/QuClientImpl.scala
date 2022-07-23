package qu.client


import qu.client.QuClient
import qu.client.backoff.BackOffPolicy
import qu.client.quorum.ClientQuorumPolicy
import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.ConcreteOperationTypes._
import qu.model.QuorumSystemThresholds

import java.util.logging.{Level, Logger}
import scala.concurrent.{ExecutionContext, Future}

import qu.LoggingUtils.AsyncLogger

/**
 * An implementation of [[qu.client.QuClient]] compatible with different backoff and quorum strategies and
 * including inline repair, repeated requests, inline repairing, OHS caching and optimistic query
 * execution optimizations.
 *
 * @param policy        the quorum policy responsible for interaction with replicas.
 * @param backoffPolicy the backoff policy used to face contention scenarios.
 * @param serversIds    the replicas ids.
 * @param thresholds    the quorum system thresholds that guarantee protocol correct semantics.
 * @tparam ObjectT       the type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization
 */
class QuClientImpl[ObjectT, Transportable[_]](private var policy: ClientQuorumPolicy[ObjectT, Transportable],
                                              private var backoffPolicy: BackOffPolicy,
                                              private val serversIds: Set[String],
                                              private val thresholds: QuorumSystemThresholds)
  extends QuClient[ObjectT, Transportable] {

  private val logger = Logger.getLogger(classOf[QuClientImpl[ObjectT, Transportable]].getName)

  var cachedOhs: OHS = emptyOhs(serversIds)

  override def submit[T](op: Operation[T, ObjectT])(implicit
                                                    ec: ExecutionContext,
                                                    transportableRequest: Transportable[Request[T, ObjectT]],
                                                    transportableResponse: Transportable[Response[Option[T]]],
                                                    transportableRequestObj: Transportable[Request[Object, ObjectT]],
                                                    transportableResponseObj: Transportable[Response[Option[Object]]]):
  Future[T] = {

    def submitWithOhs(ohs: OHS): Future[T] = {
      for {
        (answer, order, updatedOhs) <- policy.quorum[T](Some(op), ohs)
        _ <- updateCachedOhs(updatedOhs)
        answer <- if (order < thresholds.q) for {
          (opType, _, _) <- classifyAsync(updatedOhs)
          optimisticAnswer <- if (opType == METHOD) for {
            _ <- logger.logAsync(msg = "returning value of query executed optimistically (as type is METHOD but " +
              "responses' order less than q (" + order + ").")
            optimisticAnswer <- Future(
              answer.getOrElse(
                throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a " +
                  "None answer")))
          } yield optimisticAnswer else for {
            _ <- logger.logAsync(msg = "opType: " + opType + ", so repairing.")
            repairedOhs <- repair(updatedOhs)
            newAnswer <- submitWithOhs(repairedOhs)
            _ <- updateCachedOhs(updatedOhs)
          } yield newAnswer
        } yield optimisticAnswer else Future(answer.getOrElse(
          throw new RuntimeException("illegal quorum policy behaviour: user provided operations cannot have a None " +
            "answer")))
      } yield answer
    }

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
      //perform a barrier or a copy
      (_, _, ohs) <- policy.quorum(Option.empty[Operation[Object, ObjectT]],
        ohs) //here Object is fundamental as server could return other than T (could use Any too)
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType, ohs)
    } yield ohs


    def backOffAndRetryUntilMethod(operationType: ConcreteOperationTypes, ohs: OHS): Future[OHS] =
      if (operationType != METHOD) backOffAndRetry() else Future {
        ohs
      }


    //actual logic
    for {
      _ <- logger.logAsync(msg = "client starts repairing.")
      (operationType, _, _) <- classifyAsync(ohs) //could use Future.Successful here too
      ohs <- backOffAndRetryUntilMethod(operationType, ohs)
    } yield ohs
  }

  override def shutdown(): Future[Unit] = policy.shutdown()

  override def isShutdown: Flag = policy.isShutdown

}
