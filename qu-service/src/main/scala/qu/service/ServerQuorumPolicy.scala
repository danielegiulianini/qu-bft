package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.StubFactories.unencryptedDistributedJacksonStubFactory
import qu.{GrpcClientStub, JwtGrpcClientStub, RecipientInfo, ResponsesGatherer, Shutdownable}
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait ServerQuorumPolicy[Transportable[_], ObjectT] extends Shutdownable {
  def objectSync(lt: LogicalTimestamp)(implicit
                                       transportableRequest: Transportable[LogicalTimestamp],
                                       transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
  ): Future[ObjectT]
}

class SimpleServerQuorumPolicy[Transportable[_], ObjectT](servers: Map[ServerId, GrpcClientStub[Transportable]],
                                                          private val thresholds: QuorumSystemThresholds,
                                                          private val retryingTime: FiniteDuration = 3.seconds)(implicit executor: ExecutionContext)
  extends ResponsesGatherer[Transportable](servers, retryingTime)
    with ServerQuorumPolicy[Transportable, ObjectT]{

  override def objectSync(lt: LogicalTimestamp)
                         (implicit
                          transportableRequest: Transportable[LogicalTimestamp],
                          transportableResponse: Transportable[ObjectSyncResponse[ObjectT]]
                         ): Future[ObjectT] = {
    gatherResponses[LogicalTimestamp, ObjectSyncResponse[ObjectT]](lt,
      responsesQuorum = thresholds.b,
      filterSuccess = response => response.responseCode == StatusCode.SUCCESS && response.answer.isDefined
    ).map(_.values.head.answer.getOrElse(throw new Exception(" inconsistent...")))
  }
}

class JacksonSimpleBroadcastServerPolicy[ObjectT](private val thresholds: QuorumSystemThresholds,
                                                  private val servers: Map[ServerId, JwtGrpcClientStub[JavaTypeable]])(implicit executor: ExecutionContext)
  extends SimpleServerQuorumPolicy[JavaTypeable, ObjectT](servers, thresholds = thresholds) with Shutdownable


object ServerQuorumPolicy {

  type ServerQuorumPolicyFactory[Transportable[_], U] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Transportable, U] with Shutdownable


  def simpleDistributedJacksonServerQuorumFactory[U]()(implicit executor: ExecutionContext): ServerQuorumPolicyFactory[JavaTypeable, U] =
    (serversSet, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](
      servers = serversSet.map { recipientInfo =>
        id(recipientInfo) -> unencryptedDistributedJacksonStubFactory(recipientInfo.ip, recipientInfo.port, executor)
      } .toMap,
      thresholds = thresholds
    )
}