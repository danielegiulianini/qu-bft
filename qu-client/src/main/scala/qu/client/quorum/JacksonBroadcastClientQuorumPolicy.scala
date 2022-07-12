package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.SocketAddress.id
import qu.auth.Token
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.{JacksonAuthenticatedStubFactory, JwtAsyncClientStub}

import scala.concurrent.ExecutionContext

class JacksonBroadcastClientQuorumPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])
                                                 (implicit ec: ExecutionContext)
  extends BroadcastClientQuorumPolicy[ObjectT, JavaTypeable](thresholds, servers)


object JacksonBroadcastClientQuorumPolicy {

  //factory method
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
