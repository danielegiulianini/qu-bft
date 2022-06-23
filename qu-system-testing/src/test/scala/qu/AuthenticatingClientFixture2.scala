package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.client.AuthenticatingClient
import qu.service.ServersFixture
/*
trait AuthenticatingClientFixture2 extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {
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

    }
  }

}
*/
/*
import org.scalatest.{BeforeAndAfterEach, Suite}
import qu.auth.client.AuthClient
import qu.auth.common.{Authenticator, LocalAuthenticator}
import qu.auth.server.AuthServer

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

trait RemoteAuthenticatorFix extends WithAuthenticator with BeforeAndAfterEach {

  self: Suite =>

  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  override val authenticator: Authenticator = AuthClient(ip, port)
  var authServer: AuthServer = _

  val ip = "localhost"
  val port = 1000

  override def beforeEach(): Unit = {
    AuthServer(ip, port).start()
    super.beforeEach()
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally {
      authenticator match {
        case a: AuthClient => a.shutdown()
      }
      authServer.shutdown()
    }
  }

}
*/