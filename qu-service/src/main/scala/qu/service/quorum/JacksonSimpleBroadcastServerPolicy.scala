package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.Shutdownable
import qu.stub.client.JwtAsyncClientStub

import scala.concurrent.ExecutionContext

class JacksonSimpleBroadcastServerPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  private val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])(implicit executor: ExecutionContext)
  extends SimpleServerQuorumPolicy[JavaTypeable, ObjectT](servers, thresholds = thresholds) with Shutdownable
