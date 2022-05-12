package qu.protocol.model

import java.util.Objects
import scala.reflect.runtime.universe._


//could be a class separated from QUModel, bound (for LogicalTimestamp) to a ConcreteImplementation of QuModel
trait Storage {
  self: AbstractQuModel =>

  def store[T:TypeTag, U:TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Unit

  def retrieve[T:TypeTag, U:TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])]

}

trait InMemoryStorage extends Storage {
  self: AbstractQuModel =>

  private var storage: Map[(TypeTag[_], TypeTag[_]), (LogicalTimestamp, Option[Any], Any)] = Map()

  //favorites = favorites + (Objects.requireNonNull(implicitly[TypeTag[T]]) -> instance)
  override def store[T:TypeTag, U:TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])) = ???
    //storage + ( Objects.requireNonNull(implicitly[TypeTag[T]]) -> Objects.requireNonNull(implicitly[TypeTag[U]] ->)

  override def retrieve[T:TypeTag, U:TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] =
    ???
}

/*
trait PersistentStorage extends Storage {
  self: AbstractQuModel =>
}
trait PersistentCachingStorage extends Storage {
  self: AbstractQuModel =>
}
*/

