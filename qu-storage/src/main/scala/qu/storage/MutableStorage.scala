package qu.storage
import qu.model.QuorumSystemThresholdQuModel

import scala.reflect.runtime.universe._
import qu.model.QuorumSystemThresholdQuModel.LogicalTimestamp

import java.util.Objects
import scala.collection.mutable
import scala.collection.mutable.{HashMap => MutableHashMap, Map => MutableMap}
import scala.reflect.runtime.universe

abstract class MutableStorage[U: TypeTag] {
  protected val storage: MutableMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]

  def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Unit = {
    storage.getOrElseUpdate(Objects.requireNonNull(implicitly[TypeTag[T]]), new MutableHashMap()).put(logicalTimestamp, objectAndAnswer) //Map[LogicalTimestamp, (_, Option[_])]()
  }


  def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] = {
    storage.get(implicitly[TypeTag[T]]).flatMap(_.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]])
  }
}

class NonThreadSafeMutableStorage[U:TypeTag] extends MutableStorage[U] {
  override val storage: mutable.Map[universe.TypeTag[_], mutable.Map[QuorumSystemThresholdQuModel.ConcreteLogicalTimestamp, (U, Option[Any])]] = MutableMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}


