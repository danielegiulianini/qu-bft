package qu.client.backoff

import scala.concurrent.{ExecutionContext, Future}

/**
 * Backoff strategy (GoF pattern) defining backoff interval by means of an async API.
 * Useful for dealing with server not responding due to failure or high load.
 */
trait BackOffPolicy {
  def backOff()(implicit ec: ExecutionContext): Future[Unit]
}
