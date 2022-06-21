package qu

import qu.auth.server.AuthServer
import qu.client.AuthenticatingClient

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object ProvaWaitReady extends App {
  val authServerInfo = RecipientInfo(ip = "localhost", port = 1004)
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  var authServer = new AuthServer(authServerInfo.port)
  authServer.start()

  var authClient = AuthenticatingClient[Int](authServerInfo.ip,
    authServerInfo.port,
    "username",
    "password")

  authClient.register()
  authClient.authorize()
  authClient.shutdown()
  authServer.shutdown()

  authServer = new AuthServer(authServerInfo.port)
  authServer.start()
  authClient = AuthenticatingClient[Int](authServerInfo.ip,
    authServerInfo.port,
    "username",
    "password")
  authClient.register()
  authClient.authorize()
  authClient.shutdown()
  authServer.shutdown()
}
