package qu.client.backoff

import qu.OneShotAsyncScheduler

import scala.concurrent.duration.{DurationInt, FiniteDuration, MILLISECONDS}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random.nextFloat


/**
 * Implements [[qu.client.backoff.BackOffPolicy]] by increasing backoff interval exponentially and multiplying it
 * by a random factor for purposes of collision avoidance.
 * @param initialBackOffTime starting backoff interval
 * @param maxDelay maximum backoff interval
 * @param multiplier
 * @param scheduler scheduler for intervals to be scheduled
 */
class RandomExponentialBackOffPolicy(private var initialBackOffTime: FiniteDuration = 1000.millis,
                                     private var maxDelay: FiniteDuration = 5000.millis,
                                     private val multiplier: Double = 2.0,
                                     private var scheduler: OneShotAsyncScheduler) extends BackOffPolicy {
  var delay: FiniteDuration = initialBackOffTime

  def waitTime(): FiniteDuration = {
    val newWaitTime: FiniteDuration = FiniteDuration((delay * (1.0D + nextFloat() * (multiplier - 1.0D))).toMillis,
      MILLISECONDS)
    if (delay > maxDelay) {
      delay = maxDelay
    } else {
      delay = FiniteDuration((delay * multiplier).toMillis, MILLISECONDS)
    }
    newWaitTime
  }

  def backOff()(implicit ec: ExecutionContext): Future[Unit] = {
    scheduler.scheduleOnceAsPromise(waitTime())
  }
}

object RandomExponentialBackOffPolicy {
  def apply(initialBackOffTime: FiniteDuration = 1000.millis): RandomExponentialBackOffPolicy =
    new RandomExponentialBackOffPolicy(initialBackOffTime,
      maxDelay = 5000.millis, multiplier = 2.0,
      new OneShotAsyncScheduler(1))
}