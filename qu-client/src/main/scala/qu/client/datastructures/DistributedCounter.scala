package qu.client.datastructures


//this packages contains some data structures implementations built over Qu core

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.client.datastructures.Mode.ALREADY_REGISTERED
import qu.{RecipientInfo, Shutdownable}
import qu.client.{AuthenticatingClient, QuClient, QuClientBuilder}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}


trait Counter {
  /** Current value of this counter. */
  def value(): Int

  /** Increment this counter. */
  def increment(): Unit

  /** Decrement this counter. */
  protected def decrement(): Unit
}

trait Resettable {
  def reset(): Unit
}

trait ResettableCounter extends Counter with Resettable

object Value extends QueryReturningObject[Int]

object Increment extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj + 1
}

object Decrement extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj - 1
}

object Reset extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = 0
}

//also RemoteCounter is good...
class DistributedCounter(username: String,
                         password: String,
                         authServerIp: String,
                         authServerPort: Int,
                         serversInfo: Set[RecipientInfo],
                         thresholds: QuorumSystemThresholds,
                         mode: Mode = ALREADY_REGISTERED)(implicit executionContext: ExecutionContext)
  extends AuthenticatedQuClient[Int](username,
    password,
    authServerIp,
    authServerPort,
    serversInfo,
    thresholds,
    mode) with ResettableCounter with Shutdownable {

  /** Current value of this counter. */
  override def value(): Int = await(valueAsync)

  /** Increment this counter. */
  override def increment(): Unit = await(incrementAsync())

  /** Decrement this counter. */
  override def decrement(): Unit = await(decrementAsync())

  override def reset(): Unit = await(resetAsync())

  /** Current value of this counter. */
  def valueAsync: Future[Int] = {
    submit[Int](Value)
  }

  /** Increment this counter. */
  def incrementAsync(): Future[Unit] = submit[Unit](Increment)

  /** Decrement this counter. */
  protected def decrementAsync(): Future[Unit] = submit[Unit](Decrement)

  def resetAsync(): Future[Unit] = submit[Unit](Reset)
}
