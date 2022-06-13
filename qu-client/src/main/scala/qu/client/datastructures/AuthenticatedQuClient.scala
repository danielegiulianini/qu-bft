package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.{RecipientInfo, Shutdownable}
import qu.client.{QuClientImpl, AuthenticatingClient}
import qu.model.ConcreteQuModel.Operation
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

//come utilities for refactoring behaviour common to datastructures
class AuthenticatedQuClient[ObjectT](username: String,
                                     password: String,
                                     authServerIp: String, authServerPort: Int,
                                     serversInfo: Set[RecipientInfo],
                                     thresholds: QuorumSystemThresholds)(implicit executionContext: ExecutionContext)
  extends Shutdownable {

  protected def clientFuture(): Future[QuClientImpl[ObjectT, JavaTypeable]] = for {
    builder <- new AuthenticatingClient[ObjectT](authServerIp, authServerPort, username, password).authorize()
  } yield {
    serversInfo.foreach { serverInfo => builder.addServer(serverInfo.ip, serverInfo.port) }
    builder.build
  }

  //todo: fix delay,
  //todo: if not present (due to network problems) now throwing unchecked exception, (could return option.empty)
  protected def await[T](future: Future[T]): T = Await.result(future, 100.millis)

  protected def submit[ReturnValueT](operation: Operation[ReturnValueT, ObjectT]): Future[ReturnValueT] =
    clientFuture.flatMap(_.submit(operation))

  override def shutdown(): Future[Unit] = clientFuture.map(_.shutdown())

  override def isShutdown: Boolean = await[Boolean](clientFuture.map(_.isShutdown))
}
