package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.RecipientInfo
import qu.RecipientInfo.id
import qu.auth.Token
import qu.client.backoff.{BackOffPolicy, ExponentialBackOffPolicy}
import qu.client.quorum.{ClientQuorumPolicy, JacksonSimpleBroadcastClientPolicy}
import qu.model.QuorumSystemThresholds

import java.util.Objects
import scala.concurrent.ExecutionContext


case class QuClientBuilder[ObjectT, Transportable[_]]( //programmer dependencies
                                                       private val policyFactory: ClientQuorumPolicy.ClientQuorumPolicyFactory[ObjectT, Transportable],
                                                       private val backOffPolicy: BackOffPolicy,
                                                       //user dependencies
                                                       private val serversInfo: Set[RecipientInfo],
                                                       private val thresholds: Option[QuorumSystemThresholds]) {
  Objects.requireNonNull(policyFactory)
  Objects.requireNonNull(backOffPolicy)
  Objects.requireNonNull(serversInfo)
  Objects.requireNonNull(thresholds)

  def addServer(ip: String, port: Int): QuClientBuilder[ObjectT, Transportable] = {
    addServer(RecipientInfo(ip, port))
  }

  def addServer(serverInfo: RecipientInfo): QuClientBuilder[ObjectT, Transportable] = {
    //todo validation with inetsocketaddress??
    this.copy(serversInfo = serversInfo + serverInfo)
  }

  def addServers(serversInfos: Set[RecipientInfo]): QuClientBuilder[ObjectT, Transportable] = {
    //todo validation with inetsocketaddress??
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

//builder companion object specific builder instance-related utility
object QuClientBuilder {

  //choosing an implementation as the default
  def apply[U](token: Token)(implicit ec: ExecutionContext): QuClientBuilder[U, JavaTypeable] =
    new JacksonBuilderFactory().simpleBroadcastClientBuilder(token)//simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

/*
  //builder implementations
  def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: Token)(implicit ec: ExecutionContext):
  QuClientBuilder[U, JavaTypeable] =
    QuClientBuilder.empty[U, JavaTypeable](
      JacksonSimpleBroadcastClientPolicy[U](token)(_, _), //simpleJacksonPolicyFactoryUnencrypted(token) //JacksonBroadcastClientPolicy[U](token).simplePolicy(_,_)
      ExponentialBackOffPolicy())*/
}
