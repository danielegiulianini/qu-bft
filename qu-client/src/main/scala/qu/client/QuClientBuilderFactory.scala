package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.Token
import qu.client.backoff.{BackOffPolicy, ExponentialBackOffPolicy}
import qu.client.quorum.{ClientQuorumPolicy, JacksonBroadcastClientQuorumPolicy}

import scala.concurrent.ExecutionContext


/**
 * A GoF factory method for [[qu.client.QuClientBuilder]].
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
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


