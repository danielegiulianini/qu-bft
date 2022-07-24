package qu.storage

import qu.model.QuorumSystemThresholdQuModel._

import java.util.Objects
import scala.reflect.runtime.universe._


/**
 * A heterogeneous, type-safe, immutable implementation of [[qu.storage.Storage]], realized by rethinking in scala the
 * "Item 33: Consider typesafe heterogeneous containers" from Effective Java book.
 *
 * @param storage the heterogeneous map containing objects and answers.
 * @tparam ObjectT the type of the object to be stored.
 */
case class ImmutableStorage[ObjectT: TypeTag] private(private var storage: Map[TypeTag[_],
  Map[LogicalTimestamp, (ObjectT, Option[Any])]] = Map())
  extends Storage[ObjectT] {

  //Option[AnswerT] because when QU service is initialized ... the initial object must be persisted but
  //no answer is there!
  override def store[AnswerT: TypeTag](logicalTimestamp: LogicalTimestamp,
                                       objectAndAnswer: (ObjectT, Option[AnswerT])): ImmutableStorage[ObjectT] = {
    val InnerMap = storage.getOrElse(Objects.requireNonNull(implicitly[TypeTag[AnswerT]]), Map()) //Map[LogicalTimestamp, (_, Option[_])]()
    val toInsert = logicalTimestamp -> objectAndAnswer
    val updatedInnerMap = InnerMap + toInsert
    ImmutableStorage(storage + (implicitly[TypeTag[AnswerT]] -> updatedInnerMap))
  }

  //API for objects-only retrieval
  override def retrieveObject(logicalTimestamp: LogicalTimestamp): Option[ObjectT] = {
    storage
      .values
      .flatMap(_.view.filterKeys(_ == logicalTimestamp).values.map { case (obj, _) => obj })
      .headOption
  }

  override def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(ObjectT, Option[T])] = {
    val tmp = storage.get(implicitly[TypeTag[T]])
    tmp.flatMap(_.get(logicalTimestamp)
      .asInstanceOf[Option[(ObjectT, Option[T])]]) //type safe cast!
    /*with for-comprehension:
    for {
      map <- storage.get(implicitly[TypeTag[T]])
      ret <- map.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]]
    } yield ret*/
  }

}

object ImmutableStorage {
  //public factory
  def apply[ObjectT: TypeTag]() = new ImmutableStorage[ObjectT](Map())

}


