package qu

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

      //todo not shutdown properly (maybe because must shutdown the quCLientchannel too?)
      //client.shutdown()
      Await.ready(authClient.shutdown(), 30.seconds)
    }
  }

}
