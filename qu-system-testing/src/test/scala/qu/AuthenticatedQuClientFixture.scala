package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.client.QuClientImpl
import qu.service.ServersFixture

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthenticatedQuClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture with AuthenticatingClientFixture with AuthServerFixture =>

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
      //authServer.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      println("thread in lastly is: " + Thread.currentThread().getName())
      // Perform cleanup here
      println("waiting in AuthenticatedQuClientFixture")
      quClientAsFuture.map(_.shutdown())
      quClientAsFuture.map( e => println("AAAAAAAAAAAAA il quCLient is shutdown???" + e.isShutdown))

      Thread.sleep(3000)

      //Await.ready(quClientAsFuture.map(_.shutdown()), 5.seconds)
      //Await.ready(quClientAsFuture.map(_.shutdown()), 5.seconds)
    }
  }

}
