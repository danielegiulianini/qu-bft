package qu.service


import qu.model.QuorumSystemThresholds

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._

trait QuServerBuilderFactory[Transportable[_]] {
  def simpleBroadcastServerBuilder[ObjectT: TypeTag](ip: String,
                                                     port: Int,
                                                     privateKey: String,
                                                     thresholds: QuorumSystemThresholds,
                                                     obj: ObjectT)(implicit ec: ExecutionContext)
  : QuServerBuilder[Transportable, ObjectT]
}

