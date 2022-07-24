package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.SocketAddress.id
import qu.auth.Token
import qu.model.QuorumSystemThresholdQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.{JacksonAuthenticatedStubFactory, JwtAsyncClientStub}

import scala.concurrent.ExecutionContext

/**
 * Implementation of [[qu.client.quorum.BroadcastClientQuorumPolicy]] leveraging JSON and Jackson for
 * (de)serialization of protocol messages.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @param servers the client stubs by which to communicate to replicas.
 * @tparam ObjectT the type of the object replicated by Q/U servers on which operations are to be submitted.
 */
class JacksonBroadcastClientQuorumPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])
                                                 (implicit ec: ExecutionContext)
  extends BroadcastClientQuorumPolicy[ObjectT, JavaTypeable](thresholds, servers)


object JacksonBroadcastClientQuorumPolicy {

  //GoF factory method
  def apply[U](token: Token)(servers: Set[SocketAddress], thresholds: QuorumSystemThresholds)
              (implicit ec: ExecutionContext)
  : JacksonBroadcastClientQuorumPolicy[U] = {
    val factory = new JacksonAuthenticatedStubFactory()
    new JacksonBroadcastClientQuorumPolicy[U](thresholds,
      servers
        .map { recipientInfo =>
          id(recipientInfo) -> factory
            .unencryptedDistributedJwtStub(token, recipientInfo)
        }
        .toMap)
  }
}
