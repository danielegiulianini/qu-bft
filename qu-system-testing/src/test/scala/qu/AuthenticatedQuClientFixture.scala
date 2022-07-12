package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.client.QuClientImpl
import qu.service.{AbstractServersFixture, ServersFixture}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthenticatedQuClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with AbstractServersFixture with AuthenticatingClientFixture with AuthServerFixture =>

  var quClientAsFuture: Future[QuClientImpl[Int, JavaTypeable]] = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    quClientAsFuture = for {
      _ <- authClient.register()
      builder <- authClient.authorize()
    } yield builder
      .addServers(quServerIpPorts)
      .withThresholds(thresholds).build

    complete {
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      quClientAsFuture.map(_.shutdown())
      //can't wait the future here (see async***spec doc)

      Thread.sleep(3000)
    }
  }

}
