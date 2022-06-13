package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.stub.client.JwtAsyncGrpcClientStub

import scala.concurrent.ExecutionContext

class JacksonSimpleBroadcastClientPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  override protected val servers: Map[ServerId, JwtAsyncGrpcClientStub[JavaTypeable]])(implicit ec: ExecutionContext)
  extends SimpleBroadcastClientPolicy[ObjectT, JavaTypeable](thresholds, servers)
