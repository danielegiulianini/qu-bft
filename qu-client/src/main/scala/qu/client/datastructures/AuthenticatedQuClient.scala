package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.{RecipientInfo, Shutdownable}
import qu.client.datastructures.Mode.{ALREADY_REGISTERED, NOT_REGISTER}
import qu.client.{AuthenticatingClient, QuClientImpl}
import qu.model.ConcreteQuModel.Operation
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}


sealed trait Mode

case object Mode {
  object NOT_REGISTER extends Mode

  object ALREADY_REGISTERED extends Mode
}

//come utilities for refactoring behaviour common to datastructures
class AuthenticatedQuClient[ObjectT](username: String,
                                     password: String,
                                     authServerIp: String, authServerPort: Int,
                                     serversInfo: Set[RecipientInfo],
                                     thresholds: QuorumSystemThresholds,
                                     mode: Mode = ALREADY_REGISTERED)(implicit executionContext: ExecutionContext)
  extends Shutdownable {

  protected def clientFuture(): Future[QuClientImpl[ObjectT, JavaTypeable]] = {
    val authClient = new AuthenticatingClient[ObjectT](authServerIp, authServerPort, username, password)
    for {
      _ <- Future {
        mode match {
          case NOT_REGISTER => authClient.register()
          case _ =>
        }
      }
      builder <- authClient.authorize()
    } yield {
      serversInfo.foreach { serverInfo => builder.addServer(serverInfo.ip, serverInfo.port) }
      builder.build
    }
  }

  //todo: fix delay,
  //todo: if not present (due to network problems) now throwing unchecked exception, (could return option.empty)
  protected def await[T](future: Future[T]): T = Await.result(future, 5.seconds)

  protected def submit[ReturnValueT](operation: Operation[ReturnValueT, ObjectT]): Future[ReturnValueT] =
    clientFuture().flatMap(_.submit(operation))

  override def shutdown(): Future[Unit] = clientFuture.map(_.shutdown())

  override def isShutdown: Boolean = await[Boolean](clientFuture.map(_.isShutdown))
}
