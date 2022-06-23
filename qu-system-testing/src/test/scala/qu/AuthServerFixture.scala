package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.auth.server.AuthServer
import qu.service.ServersFixture

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait AuthServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>


  var authServer: AuthServer = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    authServer = new AuthServer(authServerInfo.port)

    complete {
      println("starting auth server...")
      authServer.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      /* authServer.shutdown()
       Thread.sleep(5000)*/
      println("waiting authServer (in fixture)...")
      authServer.shutdown()
      Thread.sleep(1000)

      //Await.ready(authServer.shutdown(), 5.seconds)
    }
  }
}
