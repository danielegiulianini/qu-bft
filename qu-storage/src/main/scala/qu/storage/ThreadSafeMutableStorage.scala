package qu.storage
import qu.model.QuorumSystemThresholdQuModel

import scala.reflect.runtime.universe._
import scala.collection.mutable.{Map => MutableMap}
import qu.model.QuorumSystemThresholdQuModel.LogicalTimestamp

import scala.collection.mutable
import scala.reflect.runtime.universe

class ThreadSafeMutableStorage[U:TypeTag] extends MutableStorage[U] {
  override val storage: mutable.Map[universe.TypeTag[_], mutable.Map[QuorumSystemThresholdQuModel.ConcreteLogicalTimestamp, (U, Option[Any])]] = scala.collection.concurrent.TrieMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}
