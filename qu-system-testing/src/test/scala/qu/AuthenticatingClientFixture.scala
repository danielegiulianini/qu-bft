package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.client.AuthenticatingClient
import qu.service.{AbstractServersFixture, ServersFixture}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AuthenticatingClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {
  self: AsyncTestSuite with AbstractServersFixture =>

  var authClient: AuthenticatingClient[Int] = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    authClient = AuthenticatingClient[Int](authServerInfo.ip,
      authServerInfo.port,
      "username",
      "password")

    complete {
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      authClient.shutdown()
      //can't wait the future here (see async***spec doc)
      Thread.sleep(1000)

    }
  }

}


/*
*
* package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.client.AuthenticatingClient
import qu.service.ServersFixture

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AuthenticatingClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {
  self: AsyncTestSuite with ServersFixture =>

  //todo maybe to move to fixture (or maybe all the clientFuture?) (to be shutdown  correctly)
  var authClient: AuthenticatingClient[Int] = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    authClient = AuthenticatingClient[Int](authServerInfo.ip,
      authServerInfo.port,
      "username",
      "password")

    complete {
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      println("waiting authenticatinclient (in fixture)...")
      //todo not shutdown properly (maybe because must shutdown the quCLientchannel too?)
      //Await.ready(authClient.shutdown(),  6.seconds)
      //Await.ready(authClient.shutdown(), 6.seconds)
      authClient.shutdown()
      Thread.sleep(1000)
println("AAAAAAAAAAAAAAAAAAAAAAaauthClient è sht down???" + authClient.isShutdown)
    }
  }

}

*
* */
