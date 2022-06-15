package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.auth.Token
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.stub.client.{AuthenticatedStubFactory3, JacksonAuthenticatedStubFactory, JacksonStubFactory, JwtAsyncClientStub}
import qu.{RecipientInfo, ResponsesGatherer, Shutdownable}

import scala.collection.immutable.Set
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

//import that declares specific dependency
import qu.model.ConcreteQuModel._


trait ClientQuorumPolicy[ObjectT, Transportable[_]] extends Shutdownable {
  def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
                      ohs: OHS)
                     (implicit
                      transportableRequest: Transportable[Request[AnswerT, ObjectT]],
                      transportableResponse: Transportable[Response[Option[AnswerT]]])
  : Future[(Option[AnswerT], Int, OHS)]
}


object ClientQuorumPolicy {
  //to be referenced in code when passing as HO param
  type ClientQuorumPolicyFactory[ObjectT, Transportable[_]] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable]
}
