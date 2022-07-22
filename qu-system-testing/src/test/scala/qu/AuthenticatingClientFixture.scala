package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.client.AuthenticatingClient
import qu.service.{AbstractServersFixture, ServersFixture}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthenticatingClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory{
  self: AbstractServersFixture  =>

  var authClient: AuthenticatingClient[Int] = _

  def setupAuthenticatingClient(): Future[_] = {
    println("starting authenticating...")
    authClient = AuthenticatingClient[Int](authServerInfo.ip,
      authServerInfo.port,
      "username",
      "password")
    Future{}
  }

  def tearDownAuthenticatingClient(): Future[_] =
    for {
      _ <- authClient.shutdown()
    } yield ()

  abstract override def withFixture(test: NoArgAsyncTest) = {
    new FutureOutcome(for {
      _ <- setupAuthenticatingClient()
      result <- super.withFixture(test).toFuture
      _ <- tearDownAuthenticatingClient()
    } yield result)
  }
}
