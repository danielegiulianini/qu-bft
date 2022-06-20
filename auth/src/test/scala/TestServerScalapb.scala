import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import qu.auth.client.AuthClient
import qu.auth.common.Authenticator
import qu.auth.server.AuthServer

import java.net.ServerSocket
import java.util.concurrent.Executors
import scala.concurrent.{Await, ExecutionContext}

object TestServerScalapb extends App {
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)


  val ip = "localhost"
  var port = 22 //new ServerSocket(0).getLocalPort

  val authenticator: Authenticator = AuthClient(ip, port)
  var authServer: AuthServer = _

  authServer = AuthServer(port)
  authServer.start()
  var autheClient = AuthClient(ip, port)

  //questo va:
  /*authServer.shutdown().map {
    _ => {
      authServer = AuthServer(ip, port)
      authServer.start()
    }
  }*/
  Await.ready(authServer.shutdown(), 3.seconds)
  authServer = AuthServer(port)
  authServer.start()
  println("all ok")
  /*authServer = AuthServer(ip, port)
  authServer.start()*/
}
