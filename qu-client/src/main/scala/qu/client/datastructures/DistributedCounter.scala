package qu.client.datastructures


//this packages contains some data structures implementations built over Qu core

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.client.{AuthenticatedClientBuilderInFunctionalStyle, AuthenticatingClient, QuClient}
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

class Value extends QueryReturningObject[Int]

class Increment extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj + 1
}

class Decrement extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj - 1
}

class Reset extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = 0
}

class DistributedCounter(username: String,
                         password: String,
                         authServerIp: String,
                         authServerPort: Int,
                         serversInfo: Map[String, Int],
                         thresholds: QuorumSystemThresholds)(implicit executionContext: ExecutionContext)
  extends AbstractStateMachine[Int](username,
    password,
    authServerIp,
    authServerPort,
    serversInfo,
    thresholds) with ResettableCounter {

  /** Current value of this counter. */
  override def value(): Int = await(valueAsync)

  /** Increment this counter. */
  override def increment(): Unit = await(incrementAsync())

  /** Decrement this counter. */
  override protected def decrement(): Unit = await(decrementAsync())

  override def reset(): Unit = await(resetAsync())

  /** Current value of this counter. */
  def valueAsync: Future[Int] = submit(new Value)

  /** Increment this counter. */
  def incrementAsync(): Future[Unit] = submit(new Increment)

  /** Decrement this counter. */
  protected def decrementAsync(): Future[Unit] = submit(new Decrement)

  def resetAsync(): Future[Unit] = submit(new Reset)
}
