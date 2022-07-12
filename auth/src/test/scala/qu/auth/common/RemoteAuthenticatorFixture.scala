package qu.auth.common

import org.scalatest.Suite
import qu.auth.client.AuthClient
import qu.auth.server.AuthServer

import java.net.ServerSocket
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

//to not be used with async *** spec
trait RemoteAuthenticatorFixture extends AbstractAuthenticatorFixture {

  self: Suite =>

  implicit val exec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val ip = "localhost"
  var port: Int = new ServerSocket(0).getLocalPort //leveraging ServerSocket with 0 parameter for getting the a free one

  override def createAuthenticator(): AuthClient = AuthClient(ip, port)

  var authServer: AuthServer = _

  override def beforeCreatingAuthenticator(): Unit = {
    println("beforeCreatingAuthenticator: ip and port: " + ip + port)
    //authenticator = AuthClient(ip, port)
    authServer = AuthServer(port)
    authServer.start()
  }

  protected def shutdownAuthenticator(): Unit = {
    println("shutdownAuthenticator")

    authenticator match {
      case a: AuthClient => println("shutdown .. client"); Await.ready(a.shutdown(), 5.seconds)
    }
    println("shutdown .. server...")
    Await.ready(authServer.shutdown(), 3.seconds)

  }

}
