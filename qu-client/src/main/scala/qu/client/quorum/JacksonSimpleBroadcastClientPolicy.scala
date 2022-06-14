package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo
import qu.RecipientInfo.id
import qu.auth.Token
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.{JacksonAuthenticatedStubFactory, JwtAsyncClientStub}

import scala.concurrent.ExecutionContext

class JacksonSimpleBroadcastClientPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])
                                                 (implicit ec: ExecutionContext)
  extends SimpleBroadcastClientPolicy[ObjectT, JavaTypeable](thresholds, servers)


object JacksonSimpleBroadcastClientPolicy {

  //factory method
  def apply[U](token: Token)(servers: Set[RecipientInfo], thresholds: QuorumSystemThresholds)
              (implicit ec: ExecutionContext)
  : JacksonSimpleBroadcastClientPolicy[U] =
    new JacksonSimpleBroadcastClientPolicy[U](thresholds,
      servers
        .map { recipientInfo =>
          id(recipientInfo) -> new JacksonAuthenticatedStubFactory()
            .unencryptedDistributedJwtStub(token, recipientInfo.ip, recipientInfo.port)
        }
        .toMap)
}
