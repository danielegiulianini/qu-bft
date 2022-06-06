package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.AuthClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import qu.client.AuthenticatedClientBuilder.simpleJacksonQuClientBuilderInFunctionalStyle
import qu.model.QuorumSystemThresholds

//client for providing username (or registering)
//now modeled as normal class, then could think of a builder
case class AuthenticatingClient[U](ip:String,
                                   port:Int,
                                   username: String,
                                   password: String) {
  //APIs: authorize, register, remove, edit, get, getall

  val authClient = AuthClient(ip, port)

  def register(): Future[Unit] = {  //Future[QuClient[U, JavaTypeable]]
    for {
      _ <- authClient.register(username, password)
    } yield()
  }

  def authorize():
  Future[AuthenticatedClientBuilder[U, JavaTypeable]] = {
    //se mi da il token correttamente allora ok altrimenti mi da future failed (dovrebbe già tutto essere incapsulato nella chiamata)
    for {
      token <- authClient.authorize(username, password)
    } yield simpleJacksonQuClientBuilderInFunctionalStyle[U](token = token)
  }
}
