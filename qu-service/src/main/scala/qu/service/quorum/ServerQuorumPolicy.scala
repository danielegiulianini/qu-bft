package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.stub.client.StubFactories.unencryptedDistributedJacksonStubFactory
import qu._
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}

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


  def simpleDistributedJacksonServerQuorumFactory[U]()(implicit executor: ExecutionContext): ServerQuorumPolicyFactory[JavaTypeable, U] =
    (serversSet, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](
      servers = serversSet.map { recipientInfo =>
        id(recipientInfo) -> unencryptedDistributedJacksonStubFactory(recipientInfo.ip, recipientInfo.port, executor)
      }.toMap,
      thresholds = thresholds
    )
}