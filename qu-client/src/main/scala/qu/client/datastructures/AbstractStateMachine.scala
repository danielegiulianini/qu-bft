package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.client.{AuthenticatedQuClientImpl, AuthenticatingClient}
import qu.model.ConcreteQuModel.Operation
import qu.model.QuorumSystemThresholds

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

//come utilities for refactoring behaviour common to datastructures
class AbstractStateMachine[ObjectT](username: String,
                           password: String,
                           authServerIp: String, authServerPort: Int,
                           serversInfo: Map[String, Int],
                           thresholds: QuorumSystemThresholds)(implicit executionContext: ExecutionContext) {

  protected def clientFuture: Future[AuthenticatedQuClientImpl[ObjectT, JavaTypeable]] = for {
    builder <- new AuthenticatingClient[ObjectT](authServerIp, authServerPort, username, password).authorize()
  } yield {
    serversInfo.foreach { case (ip, port) => builder.addServer(ip, port) }
    builder.build
  }

  //todo: fix delay,
  //todo: if not present (due to network problems) now throwing unchecked exception, (could return option.empty)
  protected def await[T] (future: Future[T] ): T = Await.result (future, 100.millis)

  protected def submit[ReturnValueT] (operation: Operation[ReturnValueT, ObjectT] ): Future[ReturnValueT] =
  clientFuture.flatMap (_.submit (operation) )
}
