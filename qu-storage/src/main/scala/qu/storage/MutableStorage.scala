package qu.storage
import qu.model.ConcreteQuModel

import scala.reflect.runtime.universe._
import qu.model.ConcreteQuModel.LogicalTimestamp

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
    println("lo mutable storage is :" + storage)
    storage.get(implicitly[TypeTag[T]]).flatMap(_.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]])
  }
}



//can also this be the default of qu.storage.StorageWithMutable
class NonThreadSafeMutableStorage[U:TypeTag] extends MutableStorage[U] {
  override val storage: mutable.Map[universe.TypeTag[_], mutable.Map[ConcreteQuModel.ConcreteLogicalTimestamp, (U, Option[Any])]] = MutableMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}


