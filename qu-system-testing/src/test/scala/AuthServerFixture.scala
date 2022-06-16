import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.{RecipientInfo, ServersFixture}
import qu.service.LocalQuServerCluster
import qu.service.datastructures.RemoteCounterServer

trait AuthServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>

  val authServerInfo = RecipientInfo(ip = "localhost", port = 1004) //todo to be put elsewhere the recipient Infos...

  var authServer: AuthServer = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    authServer = new AuthServer(authServerInfo.ip, authServerInfo.port)

    complete {
      authServer.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      authServer.shutdown()
    }
  }
}
