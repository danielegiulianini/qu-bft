package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.service.ServersFixture

import scala.concurrent.Await

trait QuClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture=>

  var quClient = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    authServer = new AuthServer(authServerInfo.port)

    complete {
      authServer.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      Await.ready(authServer.shutdown(), 10.seconds)
    }
  }

}
