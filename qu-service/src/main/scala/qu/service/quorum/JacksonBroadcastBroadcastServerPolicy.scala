package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress.id
import qu.model.QuorumSystemThresholdQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.{JacksonUnauthenticatedAsyncStubFactory, JwtAsyncClientStub}
import qu.{AbstractSocketAddress, Shutdownable}

import scala.concurrent.ExecutionContext

/**
 * Implementation of [[qu.service.quorum.BroadcastServerQuorumPolicy]] leveraging JSON and Jackson for
 * (de)serialization of protocol messages.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param servers the client stubs by which to communicate to replicas.
 * @tparam ObjectT type of the object replicated by Q/U servers on which operations are to be submitted.
 */
class JacksonBroadcastBroadcastServerPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                     private val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])(implicit executor: ExecutionContext)
  extends BroadcastServerQuorumPolicy[JavaTypeable, ObjectT](servers, thresholds = thresholds) with Shutdownable


object JacksonBroadcastBroadcastServerPolicy {

  def apply[U](serversSet: Set[AbstractSocketAddress],
               thresholds: QuorumSystemThresholds)
              (implicit executor: ExecutionContext): ServerQuorumPolicy[JavaTypeable, U] = {
    val jacksonFactory = new JacksonUnauthenticatedAsyncStubFactory
    new BroadcastServerQuorumPolicy[JavaTypeable, U](
      servers = serversSet.map { recipientInfo => {
        id(recipientInfo) -> jacksonFactory.unencryptedDistributedStub(recipientInfo)
      }
      }.toMap,
      thresholds = thresholds
    )
  }
}
