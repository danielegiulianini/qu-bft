package qu.service.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu._
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}
import qu.stub.client.JacksonStubFactory

import scala.concurrent.{ExecutionContext, Future}

/**
 * 
 * @tparam Transportable
 * @tparam ObjectT
 */
trait ServerQuorumPolicy[Transportable[_], ObjectT] extends Shutdownable {
  def objectSync(lt: LogicalTimestamp)(implicit
                                       transportableRequest: Transportable[LogicalTimestamp],
                                       transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
  ): Future[ObjectT]
}


object ServerQuorumPolicy {

  type ServerQuorumPolicyFactory[Transportable[_], U] =
    (Set[AbstractRecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Transportable, U]

}