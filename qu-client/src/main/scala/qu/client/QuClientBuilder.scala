package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.SocketAddress.id
import qu.auth.Token
import qu.client.backoff.{BackOffPolicy, ExponentialBackOffPolicy}
import qu.client.quorum.{ClientQuorumPolicy, JacksonBroadcastClientQuorumPolicy}
import qu.model.QuorumSystemThresholds

import java.util.Objects
import scala.concurrent.ExecutionContext

/**
 * A (GoF) builder for QuClient instances.
 * @param policyFactory the quorum policy policy to inject into the client instance.
 * @param backOffPolicy the backoff policy to inject into the client instance.
 * @param servers the ip and ports cluster replicas are listening on.
 * @param thresholds the quorum system thresholds that guarantee protocol correct semantics.
 * @tparam ObjectT the type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
case class QuClientBuilder[ObjectT, Transportable[_]]( //programmer dependencies
                                                       private val policyFactory: ClientQuorumPolicy.ClientQuorumPolicyFactory[ObjectT, Transportable],
                                                       private val backOffPolicy: BackOffPolicy,
                                                       //user dependencies
                                                       private val serversInfo: Set[SocketAddress],
                                                       private val thresholds: Option[QuorumSystemThresholds]) {
  Objects.requireNonNull(policyFactory)
  Objects.requireNonNull(backOffPolicy)
  Objects.requireNonNull(serversInfo)
  Objects.requireNonNull(thresholds)

  def addServer(ip: String, port: Int): QuClientBuilder[ObjectT, Transportable] = {
    addServer(SocketAddress(ip, port))
  }

  def addServer(serverInfo: SocketAddress): QuClientBuilder[ObjectT, Transportable] = {
    this.copy(serversInfo = serversInfo + serverInfo)
  }

  def addServers(serversInfos: Set[SocketAddress]): QuClientBuilder[ObjectT, Transportable] = {
    this.copy(serversInfo = serversInfo ++ serversInfos)
  }

  def withThresholds(thresholds: QuorumSystemThresholds): QuClientBuilder[ObjectT, Transportable] = {
    Objects.requireNonNull(thresholds)
    this.copy(thresholds = Some(thresholds))
  }

  def build: QuClientImpl[ObjectT, Transportable] = {
    require(thresholds.isDefined, "thresholds set for a client must be defined before constructing a client.")
    new QuClientImpl[ObjectT, Transportable](policyFactory(serversInfo, thresholds.get),
      backOffPolicy,
      serversInfo.map(id(_)),
      thresholds.get)
  }
}

object QuClientBuilder {

  //(Gof) factory method choosing an implementation as the default
  def apply[ObjectT](token: Token)(implicit ec: ExecutionContext): QuClientBuilder[ObjectT, JavaTypeable] =
    new JacksonQuClientBuilderFactory().simpleBroadcastClientBuilder(token)

}
