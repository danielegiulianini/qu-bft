package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import qu.auth.Token
import qu.auth.common.Constants
import qu.{RecipientInfo, Shutdownable}
import qu.client.datastructures.Mode.{ALREADY_REGISTERED, NOT_REGISTERED}
import qu.client.{AuthenticatingClient, QuClientBuilder, QuClientImpl}
import qu.model.ConcreteQuModel.{Operation, Request, Response}
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}


sealed trait Mode

case object Mode {
  object NOT_REGISTERED extends Mode

  object ALREADY_REGISTERED extends Mode
}

//come utilities for refactoring behaviour common to datastructures
class AuthenticatedQuClient[ObjectT](username: String,
                                     password: String,
                                     authServerIp: String,
                                     authServerPort: Int,
                                     serversInfo: Set[RecipientInfo],
                                     thresholds: QuorumSystemThresholds,
                                     mode: Mode = ALREADY_REGISTERED)(implicit executionContext: ExecutionContext)
  extends Shutdownable {

  private lazy val authClient = new AuthenticatingClient[ObjectT](authServerIp, authServerPort, username, password)

  private def getJwt: Token =
    Token(Jwts.builder.setSubject("ciao").signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact)

  protected lazy val clientFuture: Future[QuClientImpl[ObjectT, JavaTypeable]] = {
    println("in client future i servers are: " + serversInfo)
    for {
      /*<- Future {
        mode match {
          case NOT_REGISTERED =>
            println("la mode is NOT_REGISTERED, so registering...")
            authClient.register()
          case _ => println("la mode is ALREADY REGISTERED")
        }
      }*/
      _ <- authClient.register()

      _ <- Future {
      println ("now after registering, authorizing...")
    }
    //builder <- Future(QuClientBuilder.builder[ObjectT](getJwt))
      builder <- authClient.authorize()
    } yield builder.addServers(serversInfo).withThresholds(thresholds).build
  }


  //todo: fix delay (could be very long...)
  //todo: if not present (due to network problems) now throwing unchecked exception, (could return option.empty)
  protected def await[T](future: Future[T]): T = Await.result(future, 100.seconds)

  protected def submit[ReturnValueT](operation: Operation[ReturnValueT, ObjectT])(implicit
                                                                                  transportableRequest: JavaTypeable[Request[ReturnValueT, ObjectT]],
                                                                                  transportableResponse: JavaTypeable[Response[Option[ReturnValueT]]],
                                                                                  transportableRequestObj: JavaTypeable[Request[Object, ObjectT]],
                                                                                  transportableResponseObj: JavaTypeable[Response[Option[Object]]]): Future[ReturnValueT] = {

    clientFuture.flatMap(i => {
      println("sussmbitting" + operation + "  inside AuthenticatedQuClient");
      i.submit[ReturnValueT](operation)
    }
    ) //flatMap(_.submit(operation))
  }

  override def shutdown(): Future[Unit] = clientFuture.map(_.shutdown())

  override def isShutdown: Boolean = await[Boolean](clientFuture.map(_.isShutdown))

}