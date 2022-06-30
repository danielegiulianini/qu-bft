package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.Token
import qu.client.backoff.{BackOffPolicy, ExponentialBackOffPolicy}
import qu.client.quorum.{ClientQuorumPolicy, JacksonSimpleBroadcastClientPolicy}

import scala.concurrent.ExecutionContext

trait QuClientBuilderFactory[Transportable[_]] {
  def simpleBroadcastClientBuilder[ObjectT](token: Token)(implicit ec: ExecutionContext):
  QuClientBuilder[ObjectT, Transportable]
}

object QuClientBuilderFactory {
  def emptyBuilder[U, Transferable[_]](policyFactory: ClientQuorumPolicy.ClientQuorumPolicyFactory[U, Transferable],
                                       policy: BackOffPolicy):
  QuClientBuilder[U, Transferable] =
    QuClientBuilder(policyFactory,
      policy,
      Set(),
      Option.empty)

}


