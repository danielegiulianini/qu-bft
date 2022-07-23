package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.Shutdownable
import qu.auth.Token
import qu.model.QuorumSystemThresholds

import scala.concurrent.{ExecutionContext, Future}

//import that declares specific dependency
import qu.model.ConcreteQuModel._

/**
 * An abstract, technology-unaware, Q/U-protocol client for single-object update (see Q/U paper).
 * Allows to submit operations to a cluster in a fault-tolerant and fault-scalable fashion.
 * @tparam ObjectT type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
trait QuClient[ObjectT, Transportable[_]] extends Shutdownable {
  def submit[ReturnValueT](op: Operation[ReturnValueT, ObjectT])(implicit executionContext: ExecutionContext,
                                                                 transportableRequest: Transportable[Request[ReturnValueT, ObjectT]],
                                                                 transportableResponse: Transportable[Response[Option[ReturnValueT]]],
                                                                 transportableRequestObj: Transportable[Request[Object, ObjectT]],
                                                                 transportableResponseObj: Transportable[Response[Option[Object]]]): Future[ReturnValueT]

}

object QuClient {
  def defaultBuilder[U](token: Token)(implicit executor: ExecutionContext): QuClientBuilder[U, JavaTypeable] =
    QuClientBuilder(token)
}