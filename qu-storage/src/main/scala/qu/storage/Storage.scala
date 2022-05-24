package qu.storage

import qu.model.ConcreteQuModel.LogicalTimestamp
import scala.reflect.runtime.universe._

trait Storage[U] {
  def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Storage[U]

  def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])]

  def retrieveObject(logicalTimestamp: LogicalTimestamp): Option[U]

}