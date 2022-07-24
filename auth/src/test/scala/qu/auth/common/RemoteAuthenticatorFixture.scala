package qu.auth.common

import org.scalatest.Suite
import qu.auth.client.AuthClient
import qu.auth.server.AuthServer

import java.net.ServerSocket
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

trait RemoteAuthenticatorFixture extends AbstractAuthenticatorFixture {

  self: Suite => //to not be used with async *** spec

  implicit val exec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val ip = "localhost"
  var port: Int = 1015

  override def createAuthenticator(): AuthClient = AuthClient(ip, port)

  var authServer: AuthServer = _

  override def beforeCreatingAuthenticator(): Unit = {
    authServer = AuthServer(port)
    authServer.start()
  }

  protected def shutdownAuthenticator(): Unit = {
    authenticator match {
      case clientAuthenticator: AuthClient => Await.ready(clientAuthenticator.shutdown(), 5.seconds)
    }
    Await.ready(authServer.shutdown(), 3.seconds)
  }

}
