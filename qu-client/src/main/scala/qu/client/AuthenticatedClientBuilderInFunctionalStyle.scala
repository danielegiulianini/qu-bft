package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.Shutdownable
import qu.client.ClientQuorumPolicy.{ClientPolicyFactory, simpleJacksonPolicyFactoryUnencrypted}
import qu.model.QuorumSystemThresholds


case class AuthenticatedClientBuilderInFunctionalStyle[U, Transportable[_]]( //programmer dependencies
                                                                             private val policyFactory: (Map[String, Int], QuorumSystemThresholds) => ClientQuorumPolicy[U, Transportable] with Shutdownable,
                                                                             //user dependencies
                                                                             private val serversInfo: Map[String, Int],
                                                                             private val thresholds: Option[QuorumSystemThresholds],
                                                                           ) {
  def addServer(ip: String, port: Int): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
    this.copy(serversInfo = serversInfo + (ip -> port)) //could create channels here instead of creating in policy

  def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
    this.copy(thresholds = Some(thresholds))

  def build: AuthenticatedQuClientImpl[U, Transportable] = {
    //todo missing validation
    new AuthenticatedQuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds.get), serversInfo.keySet, thresholds.get)
  }
}

//builder companion object specific builder instance-related utility
object AuthenticatedClientBuilderInFunctionalStyle {

  def builder[U](token: String): AuthenticatedClientBuilderInFunctionalStyle[U, JavaTypeable] = simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

  private def empty[U, Transferable[_]](policyFactory: ClientPolicyFactory[Transferable, U] , token: String): AuthenticatedClientBuilderInFunctionalStyle[U, Transferable] =
    AuthenticatedClientBuilderInFunctionalStyle((mySet, thresholds) => policyFactory(mySet, thresholds), Map(), Option.empty)

  //builder implementations
  def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: String): AuthenticatedClientBuilderInFunctionalStyle[U, JavaTypeable] =
    AuthenticatedClientBuilderInFunctionalStyle.empty[U, JavaTypeable](simpleJacksonPolicyFactoryUnencrypted(token), token)
}
