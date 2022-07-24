package qu.service


import qu.model.QuorumSystemThresholds

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

/**
 * A GoF factory method for [[qu.service.QuServerBuilder]].
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
trait QuServerBuilderFactory[Transportable[_]] {
  def simpleBroadcastServerBuilder[ObjectT: TypeTag](ip: String,
                                                     port: Int,
                                                     privateKey: String,
                                                     thresholds: QuorumSystemThresholds,
                                                     obj: ObjectT)(implicit ec: ExecutionContext)
  : QuServerBuilder[Transportable, ObjectT]
}

