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
                      transportableResponse: Transportable[Response[Option[AnswerT]]]): Future[(Option[AnswerT], Int, OHS)]
}


object ClientQuorumPolicy {
  //to be referenced in code
  type ClientQuorumPolicyFactory[ObjectT, Transportable[_]] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable]
}




/*
trait Provider[Transportable[_]] {
  def ff[ObjectT](serversInfo: Set[RecipientInfo], thresholds:QuorumSystemThresholds) :ClientQuorumPolicy[ObjectT, Transportable]
}

*/

/*
trait AbstractClientQuorumPolicy2Factory[Transportable[_]]{
  protected def token: Token

  def simpleUnencryptedPolicy[ObjectT](factory: AuthenticatedStubFactory3[Transportable], servers: Set[RecipientInfo], thresholds: QuorumSystemThresholds)(implicit executionContext:ExecutionContext)
  : ClientQuorumPolicy[ObjectT, Transportable] = new SimpleBroadcastClientPolicy(thresholds,
    servers.map { recipientInfo => id(recipientInfo) -> factory.unencryptedDistributedJwtStub(token, recipientInfo.ip, recipientInfo.port) }.toMap)

}*/


/*
object ClientQuorumPolicy {

  //policy factories
  type ClientPolicyFactory[Transportable[_], ObjectT] =
    (Set[RecipientInfo], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable] with Shutdownable

  //without tls
  def simpleJacksonPolicyFactoryUnencrypted[ObjectT](jwtToken: Token)(implicit executionContext: ExecutionContext):
  ClientPolicyFactory[JavaTypeable, ObjectT] =
    simplePolicyFactoryUnencrypted(jwtToken, new JacksonAuthenticatedStubFactory)

  def simplePolicyFactoryUnencrypted[ObjectT, Transportable[_]](jwtToken: Token, factory: AuthenticatedStubFactory3[Transportable])(implicit executionContext: ExecutionContext):
  ClientPolicyFactory[Transportable, ObjectT] = {
    (servers, thresholds) =>
      new SimpleBroadcastClientPolicy(thresholds,
        servers.map { recipientInfo => id(recipientInfo) -> factory.unencryptedDistributedJwtStub(jwtToken, recipientInfo.ip, recipientInfo.port) }.toMap)
  }

  //with tls
  def simpleJacksonPolicyFactoryWithTls[U](jwtToken: String): ClientPolicyFactory[JavaTypeable, U] = ???
}*/


//another policy requiring object's preferredQuorum... (as type class)