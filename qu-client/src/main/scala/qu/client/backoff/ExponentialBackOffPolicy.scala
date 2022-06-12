package qu.client.backoff

import qu.OneShotAsyncScheduler

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class ExponentialBackOffPolicy(private var initialBackOffTime: FiniteDuration = 1000.millis,
                               private var scheduler: OneShotAsyncScheduler) extends BackOffPolicy {

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