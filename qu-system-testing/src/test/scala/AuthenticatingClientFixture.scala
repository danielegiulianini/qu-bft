import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.ServersFixture
import qu.client.AuthenticatingClient

trait AuthenticatingClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory{
  self: AsyncTestSuite with ServersFixture =>

  //todo maybe to move to fixture (or maybe all the clientFuture?) (to be shutdown  correctly)
  var client: AuthenticatingClient[Int] = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    client = AuthenticatingClient[Int](authServerInfo.ip,
      authServerInfo.port,
      "username",
      "password")

    complete {
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      //client.shutdown()
    }
  }

}
