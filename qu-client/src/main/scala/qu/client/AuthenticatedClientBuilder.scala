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


case class AuthenticatedClientBuilder[ObjectT, Transportable[_]]( //programmer dependencies
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

  def addServer(ip: String, port: Int): AuthenticatedClientBuilder[ObjectT, Transportable] = {
    //todo validation with inetsocketaddress??
    this.copy(serversInfo = serversInfo + RecipientInfo(ip, port))
  }

  def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilder[ObjectT, Transportable] = {
    Objects.requireNonNull(thresholds)
    this.copy(thresholds = Some(thresholds))
  }

  def build: AuthenticatedQuClientImpl[ObjectT, Transportable] = {
    new AuthenticatedQuClientImpl[ObjectT, Transportable](policyFactory(serversInfo, thresholds.get),
      backOffPolicy,
      serversInfo.map(id(_)),
      thresholds.get)
  }
}

//builder companion object specific builder instance-related utility
object AuthenticatedClientBuilder {

  //choosing an implementation as the default
  def builder[U](token: Token)(implicit ec:ExecutionContext): AuthenticatedClientBuilder[U, JavaTypeable] =
    simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

  private def empty[U, Transferable[_]](policyFactory: ClientPolicyFactory[Transferable, U],
                                        policy: BackOffPolicy):
  AuthenticatedClientBuilder[U, Transferable] =
    AuthenticatedClientBuilder((mySet, thresholds) => policyFactory(mySet, thresholds),
      policy,
      Set(),
      Option.empty)

  //builder implementations
  def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: Token)(implicit ec:ExecutionContext):
  AuthenticatedClientBuilder[U, JavaTypeable] =
    AuthenticatedClientBuilder.empty[U, JavaTypeable](
      simpleJacksonPolicyFactoryUnencrypted(token),
      ExponentialBackOffPolicy())
}
