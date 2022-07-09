package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.QuServiceBuilder.JacksonServiceBuilderFactory
import scala.reflect.runtime.universe._

import scala.concurrent.ExecutionContext

class JacksonServerBuilderFactory extends QuServerBuilderFactory[JavaTypeable] {
  override def simpleBroadcastClientBuilder[ObjectT:TypeTag](ip: String,
                                                     port: Int,
                                                     privateKey: String,
                                                     thresholds: QuorumSystemThresholds,
                                                     obj: ObjectT)(implicit ec: ExecutionContext)
  : QuServerBuilder[JavaTypeable, ObjectT] = new QuServerBuilder[JavaTypeable, ObjectT](
    new JacksonServiceBuilderFactory(),
    new JwtAuthorizationServerInterceptor(),
    ip,
    port,
    privateKey,
    thresholds,
    obj)
}
