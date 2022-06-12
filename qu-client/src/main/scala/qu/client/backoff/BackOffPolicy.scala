package qu.client.backoff

import scala.concurrent.{ExecutionContext, Future}

trait BackOffPolicy {
  def backOff()(implicit ec: ExecutionContext): Future[Unit]
}
