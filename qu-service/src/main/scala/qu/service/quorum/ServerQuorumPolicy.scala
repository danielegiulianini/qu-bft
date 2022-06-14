package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu._
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}
import qu.stub.client.JacksonStubFactory

import scala.concurrent.{ExecutionContext, Future}

trait ServerQuorumPolicy[Transportable[_], ObjectT] extends Shutdownable {
  def objectSync(lt: LogicalTimestamp)(implicit
                                       transportableRequest: Transportable[LogicalTimestamp],
                                       transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
  ): Future[ObjectT]
}


object ServerQuorumPolicy {

  type ServerQuorumPolicyFactory[Transportable[_], U] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Transportable, U] with Shutdownable


  def simpleDistributedJacksonServerQuorumFactory[U]()(implicit executor: ExecutionContext): ServerQuorumPolicyFactory[JavaTypeable, U] = {
    val jacksonFactory = new JacksonStubFactory
    (serversSet, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](
      servers = serversSet.map { recipientInfo =>
        id(recipientInfo) -> jacksonFactory.unencryptedDistributedStub(recipientInfo.ip, recipientInfo.port)
      }.toMap,
      thresholds = thresholds
    )
  }
}