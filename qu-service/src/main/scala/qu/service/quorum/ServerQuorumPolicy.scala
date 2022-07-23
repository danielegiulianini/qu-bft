package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress.id
import qu._
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}
import qu.stub.client.JacksonUnauthenticatedAsyncStubFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * Strategy (GoF pattern) responsible for object syncing, namely for: selecting replicas to contact
 * and actuating retransmission logic, so managing all the server-server interaction logic.
 * @tparam ObjectT type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
trait ServerQuorumPolicy[Transportable[_], ObjectT] extends Shutdownable {
  def objectSync(lt: LogicalTimestamp)(implicit
                                       transportableRequest: Transportable[LogicalTimestamp],
                                       transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
  ): Future[ObjectT]
}


object ServerQuorumPolicy {

  type ServerQuorumPolicyFactory[Transportable[_], U] =
    (Set[AbstractSocketAddress], QuorumSystemThresholds) => ServerQuorumPolicy[Transportable, U]

}