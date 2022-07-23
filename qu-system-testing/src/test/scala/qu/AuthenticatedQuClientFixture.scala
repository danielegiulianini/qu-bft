package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.client.QuClientImpl
import qu.service.datastructures.RemoteCounterServer
import qu.service.{AbstractServersFixture, LocalQuServerCluster, ServersFixture}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthenticatedQuClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AbstractServersFixture with AuthenticatingClientFixture with AuthServerFixture =>

  var quClientAsFuture: Future[QuClientImpl[Int, JavaTypeable]] = _

  // Do whatever setup you need here.
  def setupAuthenticatedQuClient(): Future[_] = {
    quClientAsFuture = for {
      quClientAsFuture <- for {
        _ <- authClient.register()
        builder <- authClient.authorize()
        a <- Future {
          builder
            .addServers(quServerIpPorts)
            .withThresholds(thresholds).build
        }
      } yield a
    } yield quClientAsFuture
    quClientAsFuture
  }


  // Cleanup whatever you need here.
  def tearDownAuthenticatedQuClient(): Future[_] = for {
    quClient <- quClientAsFuture
    _ <- quClient.shutdown()
  } yield ()

  abstract override def withFixture(test: NoArgAsyncTest) = {
    new FutureOutcome(for {
      _ <- setupAuthenticatedQuClient()
      result <- super.withFixture(test).toFuture
      _ <- tearDownAuthenticatedQuClient()
    } yield result)
  }
}
