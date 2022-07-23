package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.Token
import qu.client.backoff.{ExponentialBackOffPolicy, RandomExponentialBackOffPolicy}
import qu.client.quorum.JacksonBroadcastClientQuorumPolicy

import scala.concurrent.ExecutionContext

class JacksonQuClientBuilderFactory extends QuClientBuilderFactory[JavaTypeable] {
  override def simpleBroadcastClientBuilder[ObjectT](token: Token)(implicit ec: ExecutionContext)
  : QuClientBuilder[ObjectT, JavaTypeable] =
  QuClientBuilderFactory.emptyBuilder[ObjectT, JavaTypeable](
    JacksonBroadcastClientQuorumPolicy[ObjectT](token)(_, _), //simpleJacksonPolicyFactoryUnencrypted(token) //JacksonBroadcastClientPolicy[U](token).simplePolicy(_,_)
    RandomExponentialBackOffPolicy())
}
