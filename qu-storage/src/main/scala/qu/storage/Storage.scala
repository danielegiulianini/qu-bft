package qu.storage

import qu.model.QuorumSystemThresholdQuModel.LogicalTimestamp
import scala.reflect.runtime.universe._


/**
 * A storage for objects and answers (indexed by logical timestamp) resulting from operations invoked on them.
 * @tparam ObjectT the type of the object to be stored.
 */
trait Storage[ObjectT] {
  def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (ObjectT, Option[T])): Storage[ObjectT]

  def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(ObjectT, Option[T])]

  def retrieveObject(logicalTimestamp: LogicalTimestamp): Option[ObjectT]

}