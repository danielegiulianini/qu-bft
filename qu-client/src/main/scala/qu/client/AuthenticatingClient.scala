package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.AuthClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import qu.client.AuthenticatedClientBuilder.simpleJacksonQuClientBuilderInFunctionalStyle
import qu.model.QuorumSystemThresholds

//client for providing username (or registering)
//now modeled as normal class, then could think of a builder
case class AuthenticatingClient[U](ip:String, port:Int,
                                   username: String,
                                   password: String) {
  //creates the channel and passes it to stubs

  //APIs: authorize, register, remove, edit, get, getall
 // AuthClient()

  def register(): Future[QuClient[U, JavaTypeable]] = {
    //call to auth service...
    //for { registered <- register }
    null
  }

  def authorize():
  Future[AuthenticatedClientBuilder[U, JavaTypeable]] = {
    //se mi da il token correttamente allora ok altrimenti mi da future failed (dovrebbe già
    //tutto essere incapsulato nella chiamata)
    val token = ""

    //call to auth service...
    //it's to be inserted in for comprehension
    Future {
      simpleJacksonQuClientBuilderInFunctionalStyle[U](token = token)
    }
  }
}
