package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress.id
import qu.model.ConcreteQuModel.ServerId
import qu.model.QuorumSystemThresholds
import qu.{AbstractSocketAddress, SocketAddress, Shutdownable}
import qu.service.AbstractQuService.ServerInfo
import qu.service.quorum.ServerQuorumPolicy.ServerQuorumPolicyFactory
import qu.stub.client.{JacksonStubFactory, JwtAsyncClientStub}

import scala.concurrent.ExecutionContext

class JacksonBroadcastBroadcastServerPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                     private val servers: Map[ServerId, JwtAsyncClientStub[JavaTypeable]])(implicit executor: ExecutionContext)
  extends BroadcastServerQuorumPolicy[JavaTypeable, ObjectT](servers, thresholds = thresholds) with Shutdownable


object JacksonBroadcastBroadcastServerPolicy {

  //ALTRA POSSIBILITà
  //factory method
  /*def apply[U]()(implicit executor: ExecutionContext): ServerQuorumPolicyFactory[JavaTypeable, U] = {
    val jacksonFactory = new JacksonStubFactory
    (serversSet, thresholds) =>
      new SimpleServerQuorumPolicy[JavaTypeable, U](
        servers = serversSet.map { recipientInfo =>
          id(recipientInfo) -> jacksonFactory.unencryptedDistributedStub(recipientInfo)
        }.toMap,
        thresholds = thresholds
      )
  }*/

  def apply[U](sourceSid: ServerId,
               serversSet: Set[AbstractSocketAddress],
               thresholds: QuorumSystemThresholds)
              (implicit executor: ExecutionContext): ServerQuorumPolicy[JavaTypeable, U] = {
    val jacksonFactory = new JacksonStubFactory
    new BroadcastServerQuorumPolicy[JavaTypeable, U](
      servers = serversSet.map { recipientInfo => {
        id(recipientInfo) -> jacksonFactory.unencryptedDistributedStub(recipientInfo)
      }
      }.toMap,
      thresholds = thresholds
    )
  }
}
