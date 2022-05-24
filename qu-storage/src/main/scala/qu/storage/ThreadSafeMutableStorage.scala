package qu.storage
import qu.model.ConcreteQuModel

import scala.reflect.runtime.universe._
import scala.collection.mutable.{Map => MutableMap}
import qu.model.ConcreteQuModel.LogicalTimestamp

import scala.collection.mutable
import scala.reflect.runtime.universe

class ThreadSafeMutableStorage[U:TypeTag] extends MutableStorage[U] {
  override val storage: mutable.Map[universe.TypeTag[_], mutable.Map[ConcreteQuModel.MyLogicalTimestamp, (U, Option[Any])]] = scala.collection.concurrent.TrieMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}
