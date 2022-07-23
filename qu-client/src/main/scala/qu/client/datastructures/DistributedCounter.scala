package qu.client.datastructures


//this packages contains some data structures implementations built over Qu core

import qu.client.datastructures.Mode.ALREADY_REGISTERED
import qu.{SocketAddress, Shutdownable}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}


object Value extends QueryReturningObject[Int]

case class Increment() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj + 1
}

case class Decrement() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = obj - 1
}

case class Reset() extends UpdateReturningUnit[Int] {
  override def updateObject(obj: Int): Int = 0
}

case class DistributedCounter(username: String,
                         password: String,
                         authServerIp: String,
                         authServerPort: Int,
                         serversInfo: Set[SocketAddress],
                         thresholds: QuorumSystemThresholds,
                         mode: Mode = ALREADY_REGISTERED,
                         maxTimeToWait: Duration = 100.seconds)(implicit executionContext: ExecutionContext)
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
  def incrementAsync(): Future[Unit] = submit[Unit](Increment())

  /** Decrement this counter. */
  def decrementAsync(): Future[Unit] = submit[Unit](Decrement())

  def resetAsync(): Future[Unit] = submit[Unit](Reset())
}
