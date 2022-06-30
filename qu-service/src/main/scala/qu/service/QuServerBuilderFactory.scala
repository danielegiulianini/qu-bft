package qu.service


import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.model.QuorumSystemThresholds

import scala.concurrent.ExecutionContext

trait QuServerBuilderFactory[Transportable[_]] {
  def simpleBroadcastClientBuilder[ObjectT](ip: String,
                                            port: Int,
                                            privateKey: String,
                                            thresholds: QuorumSystemThresholds,
                                            obj: ObjectT)(implicit ec: ExecutionContext)
  : QuServerBuilder[JavaTypeable, ObjectT]
}

