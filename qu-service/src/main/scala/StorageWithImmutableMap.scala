import qu.protocol.model.ConcreteQuModel._

import scala.reflect.runtime.universe._
import java.util.Objects


//from Effective Java, "Item 33: Consider typesafe heterogeneous containers":
//storage separate from QUModel (this class can be generic in U(ObjecT)
//todo could be futhermore split (in general heterogeneous container, and in another using it but specific(with lts etc...))
class StorageWithImmutableMap[U: TypeTag] {
  private var storage: Map[TypeTag[_], Map[LogicalTimestamp, (U, Option[Any])]] = Map()

  def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Unit = {
    //se c' quell'aentrata allora c'p la mappa quindi aggiungi alla mappa
    //se non c'è allora crea una nuova mappa con quella entrata (stesso discorso dellacached)

    //sync needed
    this.synchronized {
      val InnerMap = storage.getOrElse(Objects.requireNonNull(implicitly[TypeTag[T]]), Map()) //Map[LogicalTimestamp, (_, Option[_])]()
      val toInsert = logicalTimestamp -> objectAndAnswer
      val updtInnerMap = InnerMap + toInsert
      storage = storage + (implicitly[TypeTag[T]] -> updtInnerMap)
    }
  }


  def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] = {
    //todo really needed?
    val tmp = synchronized {
      storage.get(implicitly[TypeTag[T]])
    }

    tmp.flatMap(_.get(logicalTimestamp)
      .asInstanceOf[Option[(U, Option[T])]])
    /*// with for-comprehension:
    for {
      map <- storage.get(implicitly[TypeTag[T]])
      ret <- map.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]]
    } yield ret*/
  }

}


import collection.mutable.{Map => MutableMap}
import collection.mutable.{HashMap => MutableHashMap}

abstract class StorageWithMutable[U: TypeTag] {
  protected val storage: MutableMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]

  def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Unit =
    storage.getOrElseUpdate(Objects.requireNonNull(implicitly[TypeTag[T]]), new MutableHashMap()).put(logicalTimestamp, objectAndAnswer) //Map[LogicalTimestamp, (_, Option[_])]()

  def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] =
    storage.get(implicitly[TypeTag[T]]).flatMap(_.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]])
}

class ThreadSafeStorageWithMutable[U:TypeTag] extends StorageWithMutable[U] {
  override val storage = scala.collection.concurrent.TrieMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}

//can also this be the default of StorageWithMutable
class NonThreadSafeStorageWithMutable[U:TypeTag] extends StorageWithMutable[U] {
  override val storage = MutableMap[TypeTag[_], MutableMap[LogicalTimestamp, (U, Option[Any])]]()
}




object UseCase extends App {
  val storage = new StorageWithImmutableMap[Int]()
  val myLt = MyLogicalTimestamp(2, false, Some("id1"), Option.empty, Option.empty)
  storage.store[String](myLt, (2, Some("io"))) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  val retrieved = storage.retrieve[String](myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrieved is: " + retrieved)


  println("now testing mutableStorage")
  val mutableStorage = new ThreadSafeStorageWithMutable[Int]()
  val myLt2 = MyLogicalTimestamp(2, false, Some("id1"), Option.empty, Option.empty)
  storage.store[String](myLt, (2, Some("io"))) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  val retrieved2 = storage.retrieve[String](myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrieved is: " + retrieved2)

}


