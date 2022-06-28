package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.auth.Token
import qu.client.backoff.ExponentialBackOffPolicy
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy

import scala.concurrent.ExecutionContext

class JacksonBuilderFactory extends BuilderFactory[JavaTypeable] {
  override def simpleBroadcastClientBuilder[ObjectT](token: Token)(implicit ec: ExecutionContext)
  : QuClientBuilder[ObjectT, JavaTypeable] =
  BuilderFactory.emptyBuilder[ObjectT, JavaTypeable](
    JacksonSimpleBroadcastClientPolicy[ObjectT](token)(_, _), //simpleJacksonPolicyFactoryUnencrypted(token) //JacksonBroadcastClientPolicy[U](token).simplePolicy(_,_)
    ExponentialBackOffPolicy())
}
