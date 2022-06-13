package qu.client.quorum

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.stub.client.RecipientInfo.id
import qu.stub.client.StubFactories.distributedJacksonJwtStubFactory
import qu.auth.Token
import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.stub.client.{JwtAsyncClientStub, RecipientInfo}
import qu.{ResponsesGatherer, Shutdownable}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

//import that declares specific dependency
import qu.model.ConcreteQuModel._


trait ClientQuorumPolicy[ObjectT, Transportable[_]] extends Shutdownable {
  def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
                      ohs: OHS)
                     (implicit
                      transportableRequest: Transportable[Request[AnswerT, ObjectT]],
                      transportableResponse: Transportable[Response[Option[AnswerT]]]): Future[(Option[AnswerT], Int, OHS)]
}







object ClientQuorumPolicy {

  //policy factories
  type ClientPolicyFactory[Transportable[_], ObjectT] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable] with Shutdownable

  //without tls
  def simpleJacksonPolicyFactoryUnencrypted[ObjectT](jwtToken: Token)(implicit executionContext: ExecutionContext): ClientPolicyFactory[JavaTypeable, ObjectT] =
    (servers, thresholds) => new SimpleBroadcastClientPolicy(thresholds,
      servers.map { recipientInfo => id(recipientInfo) -> distributedJacksonJwtStubFactory(jwtToken, recipientInfo.ip, recipientInfo.port, executionContext) }.toMap)

  //with tls
  def simpleJacksonPolicyFactoryWithTls[U](jwtToken: String): ClientPolicyFactory[JavaTypeable, U] = ???
}


//another policy requiring object's preferredQuorum... (as type class)