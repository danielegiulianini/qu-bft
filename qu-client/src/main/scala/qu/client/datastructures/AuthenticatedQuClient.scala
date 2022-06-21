package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.{RecipientInfo, Shutdownable}
import qu.client.datastructures.Mode.{ALREADY_REGISTERED, NOT_REGISTERED}
import qu.client.{AuthenticatingClient, QuClientImpl}
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

  protected lazy val clientFuture: Future[QuClientImpl[ObjectT, JavaTypeable]] = {
    for {
      _ <- mode match {
        case NOT_REGISTERED =>
          authClient.register()
        case _ =>
          Future.unit
      }
      builder <- authClient.authorize()
    } yield builder.addServers(serversInfo).withThresholds(thresholds).build
  }

  //todo: fix delay (could be very long... (due to backoffs etc...)
  //todo: if not present (due to network problems) now throwing unchecked exception, (could return option.empty)
  protected def await[T](future: Future[T]): T = Await.result(future, 100.seconds)

  protected def submit[ReturnValueT](operation: Operation[ReturnValueT, ObjectT])(implicit
                                                                                  transportableRequest: JavaTypeable[Request[ReturnValueT, ObjectT]],
                                                                                  transportableResponse: JavaTypeable[Response[Option[ReturnValueT]]],
                                                                                  transportableRequestObj: JavaTypeable[Request[Object, ObjectT]],
                                                                                  transportableResponseObj: JavaTypeable[Response[Option[Object]]]): Future[ReturnValueT] = {

    clientFuture.flatMap(_.submit(operation))
  }

  override def shutdown(): Future[Unit] = clientFuture.map(_.shutdown())

  override def isShutdown: Boolean = await[Boolean](clientFuture.map(_.isShutdown))

}