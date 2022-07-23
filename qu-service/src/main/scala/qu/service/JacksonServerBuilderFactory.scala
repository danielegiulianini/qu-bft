package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.QuorumSystemThresholds
import qu.service.AbstractGrpcQuService.QuServiceBuilder.JacksonBroadcastServiceBuilderFactory
import scala.reflect.runtime.universe._

import scala.concurrent.ExecutionContext


/**
 * A Jackson implementation for [[qu.service.QuServerBuilderFactory]] which creates Q/U servers that
 * broadcasts to other replicas when object-syncing.
 */
class JacksonServerBuilderFactory extends QuServerBuilderFactory[JavaTypeable] {
  override def simpleBroadcastServerBuilder[ObjectT:TypeTag](ip: String,
                                                             port: Int,
                                                             privateKey: String,
                                                             thresholds: QuorumSystemThresholds,
                                                             obj: ObjectT)(implicit ec: ExecutionContext)
  : QuServerBuilder[JavaTypeable, ObjectT] = new QuServerBuilder[JavaTypeable, ObjectT](
    new JacksonBroadcastServiceBuilderFactory(),
    new JwtAuthorizationServerInterceptor(),
    ip,
    port,
    privateKey,
    thresholds,
    obj)
}
