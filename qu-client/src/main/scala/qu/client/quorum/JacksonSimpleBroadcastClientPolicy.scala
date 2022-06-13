package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.JwtAsyncClientStub

import scala.concurrent.ExecutionContext

class JacksonSimpleBroadcastClientPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])(implicit ec: ExecutionContext)
  extends SimpleBroadcastClientPolicy[ObjectT, JavaTypeable](thresholds, servers)
