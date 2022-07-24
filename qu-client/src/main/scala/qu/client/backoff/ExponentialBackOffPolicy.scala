package qu.client.backoff

import qu.OneShotAsyncScheduler

import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Implements [[qu.client.backoff.BackOffPolicy]] by increasing backoff interval exponentially.
 * @param initialBackOffTime starting backoff interval
 * @param scheduler scheduler for intervals to be scheduled
 */
class ExponentialBackOffPolicy(private var initialBackOffTime: FiniteDuration = 1000.millis,
                               private val scheduler: OneShotAsyncScheduler,
                               private val multiplier: Int = 2) extends BackOffPolicy {


  def waitTime(): FiniteDuration = {
    initialBackOffTime *= multiplier
    initialBackOffTime
  }

  def backOff()(implicit ec: ExecutionContext): Future[Unit] = {
    scheduler.scheduleOnceAsPromise(waitTime())
  }
}





object ExponentialBackOffPolicy {
  def apply(initialBackOffTime: FiniteDuration = 1000.millis): ExponentialBackOffPolicy =
    new ExponentialBackOffPolicy(initialBackOffTime, new OneShotAsyncScheduler(1))
}
