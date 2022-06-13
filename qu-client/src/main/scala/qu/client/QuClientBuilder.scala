package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.auth.Token
import qu.{RecipientInfo, Shutdownable}
import qu.client.quorum.ClientQuorumPolicy.{ClientPolicyFactory, simpleJacksonPolicyFactoryUnencrypted}
import qu.client.backoff.{BackOffPolicy, ExponentialBackOffPolicy}
import qu.model.QuorumSystemThresholds

import java.util.Objects
import scala.concurrent.ExecutionContext


case class QuClientBuilder[ObjectT, Transportable[_]]( //programmer dependencies
                                                       private val policyFactory: ClientPolicyFactory[Transportable, ObjectT],
                                                       private val backOffPolicy: BackOffPolicy,
                                                       //user dependencies
                                                       private val serversInfo: Set[RecipientInfo],
                                                       private val thresholds: Option[QuorumSystemThresholds],
                                                                ) {
  Objects.requireNonNull(policyFactory)
  Objects.requireNonNull(backOffPolicy)
  Objects.requireNonNull(serversInfo)
  Objects.requireNonNull(thresholds)

  def addServer(ip: String, port: Int): QuClientBuilder[ObjectT, Transportable] = {
    //todo validation with inetsocketaddress??
    this.copy(serversInfo = serversInfo + RecipientInfo(ip, port))
  }

  def withThresholds(thresholds: QuorumSystemThresholds): QuClientBuilder[ObjectT, Transportable] = {
    Objects.requireNonNull(thresholds)
    this.copy(thresholds = Some(thresholds))
  }

  def build: QuClientImpl[ObjectT, Transportable] = {
    new QuClientImpl[ObjectT, Transportable](policyFactory(serversInfo, thresholds.get),
      backOffPolicy,
      serversInfo.map(id(_)),
      thresholds.get)
  }
}

//builder companion object specific builder instance-related utility
object QuClientBuilder {

  //choosing an implementation as the default
  def builder[U](token: Token)(implicit ec:ExecutionContext): QuClientBuilder[U, JavaTypeable] =
    simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

  private def empty[U, Transferable[_]](policyFactory: ClientPolicyFactory[Transferable, U],
                                        policy: BackOffPolicy):
  QuClientBuilder[U, Transferable] =
    QuClientBuilder((mySet, thresholds) => policyFactory(mySet, thresholds),
      policy,
      Set(),
      Option.empty)

  //builder implementations
  def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: Token)(implicit ec:ExecutionContext):
  QuClientBuilder[U, JavaTypeable] =
    QuClientBuilder.empty[U, JavaTypeable](
      simpleJacksonPolicyFactoryUnencrypted(token),
      ExponentialBackOffPolicy())
}
