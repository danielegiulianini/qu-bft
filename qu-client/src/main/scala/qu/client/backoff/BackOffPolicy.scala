package qu.client.backoff

import scala.concurrent.{ExecutionContext, Future}

//application-level retry policy for dealing with server not responding fue to failure or high load
trait BackOffPolicy {
  def backOff()(implicit ec: ExecutionContext): Future[Unit]
}
