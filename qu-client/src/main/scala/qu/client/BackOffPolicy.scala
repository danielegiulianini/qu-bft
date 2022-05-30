package qu.client

import qu.OneShotAsyncScheduler

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

trait BackOffPolicy {
  def backOff()(implicit ec: ExecutionContext): Future[Void]
}

class ExponentialBackOffPolicy(private var initialBackOffTime: FiniteDuration = 1000.millis,
                               private var scheduler: OneShotAsyncScheduler) {

  // todo (could use functional object)
  def backOff()(implicit ec: ExecutionContext): Future[Unit] = {
    initialBackOffTime *= 2
    scheduler.scheduleOnceAsPromise(initialBackOffTime)
  }
}

object ExponentialBackOffPolicy {
  def apply(initialBackOffTime: FiniteDuration = 1000.millis): ExponentialBackOffPolicy =
    new ExponentialBackOffPolicy(initialBackOffTime, new OneShotAsyncScheduler(1))
}
