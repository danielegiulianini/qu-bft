package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.auth.server.AuthServer
import qu.service.{AbstractServersFixture, ServersFixture}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AbstractServersFixture =>


  var authServer: AuthServer = _


  def setupAuthServer(): Future[_] = {
    authServer = new AuthServer(authServerInfo.port)

    Future {
      authServer.start()
    }
  }


  def tearDownAuthServer(): Future[_] =
    authServer.shutdown()

  abstract override def withFixture(test: NoArgAsyncTest) = {
    new FutureOutcome(for {
      _ <- setupAuthServer()
      result <- super.withFixture(test).toFuture
      _ <- tearDownAuthServer()
    } yield result)
  }
}
