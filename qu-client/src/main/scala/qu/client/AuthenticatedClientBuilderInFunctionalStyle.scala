package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo.id
import qu.{RecipientInfo, Shutdownable}
import qu.client.ClientQuorumPolicy.{ClientPolicyFactory, simpleJacksonPolicyFactoryUnencrypted}
import qu.model.QuorumSystemThresholds


case class AuthenticatedClientBuilderInFunctionalStyle[ObjectT, Transportable[_]]( //programmer dependencies
                                                                                   private val policyFactory: ClientPolicyFactory[Transportable, ObjectT],
                                                                                   private val backOffPolicy: BackOffPolicy,
                                                                                   //user dependencies
                                                                                   private val serversInfo: Set[RecipientInfo],
                                                                                   private val thresholds: Option[QuorumSystemThresholds],
                                                                                 ) {

  def addServer(ip: String, port: Int): AuthenticatedClientBuilderInFunctionalStyle[ObjectT, Transportable] =
    this.copy(serversInfo = serversInfo + RecipientInfo(ip, port)) //could create channels here instead of creating in policy

  def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilderInFunctionalStyle[ObjectT, Transportable] =
    this.copy(thresholds = Some(thresholds))

  def build: AuthenticatedQuClientImpl[ObjectT, Transportable] = {
    
    //todo missing validation
    new AuthenticatedQuClientImpl[ObjectT, Transportable](policyFactory(serversInfo, thresholds.get),
      backOffPolicy,
      serversInfo.map(id(_)),
      thresholds.get)
  }
}

//builder companion object specific builder instance-related utility
object AuthenticatedClientBuilderInFunctionalStyle {

  def builder[U](token: String): AuthenticatedClientBuilderInFunctionalStyle[U, JavaTypeable] =
    simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

  private def empty[U, Transferable[_]](policyFactory: ClientPolicyFactory[Transferable, U],
                                        policy: BackOffPolicy):
  AuthenticatedClientBuilderInFunctionalStyle[U, Transferable] =
    AuthenticatedClientBuilderInFunctionalStyle((mySet, thresholds) => policyFactory(mySet, thresholds),
      policy,
      Set(),
      Option.empty)

  //builder implementations
  def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: String):
  AuthenticatedClientBuilderInFunctionalStyle[U, JavaTypeable] =
    AuthenticatedClientBuilderInFunctionalStyle.empty[U, JavaTypeable](
      simpleJacksonPolicyFactoryUnencrypted(token),
      ExponentialBackOffPolicy())
}
