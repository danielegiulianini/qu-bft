package qu.storage

import qu.model.ConcreteQuModel._

import java.util.Objects
import scala.reflect.runtime.universe._


//from Effective Java, "Item 33: Consider typesafe heterogeneous containers":
//storage separate from QUModel (this class can be generic in U(ObjecT)
//todo could be futhermore split (in general heterogeneous container, and in another using it but specific(with lts etc...))
//since it's immutable it demands to caller (user) the burden for thread sadety
class ImmutableStorage[U: TypeTag] private(private var storage:
                                           Map[TypeTag[_], Map[LogicalTimestamp, (U, Option[Any])]] = Map())
  extends Storage[U] {

  //qhy store could have option[response]?? for the first time ... the initial object!
  override def store[T: TypeTag](logicalTimestamp: LogicalTimestamp,
                                 objectAndAnswer: (U, Option[T])): ImmutableStorage[U] = {
    println("from storage, storing with lt: " + logicalTimestamp)

    //se c' quell'aentrata allora c'p la mappa quindi aggiungi alla mappa
    //se non c'è allora crea una nuova mappa con quella entrata (stesso discorso dellacached)
    //println("lo storage before update is :" + storage)
    val InnerMap = storage.getOrElse(Objects.requireNonNull(implicitly[TypeTag[T]]), Map()) //Map[LogicalTimestamp, (_, Option[_])]()
    val toInsert = logicalTimestamp -> objectAndAnswer
    val updtInnerMap = InnerMap + toInsert
    //println("afterUpdate  storage is :" + storage + (implicitly[TypeTag[T]] -> updtInnerMap))
    new ImmutableStorage(storage + (implicitly[TypeTag[T]] -> updtInnerMap))
  }

  //new API, for objects only
  override def retrieveObject(logicalTimestamp: LogicalTimestamp): Option[U] = {
    println("from storage, retrieving with lt: " + logicalTimestamp)
    storage
      .values
      .flatMap(_.view.filterKeys(_ == logicalTimestamp).values.map { case (obj, _) => obj })
      .headOption
  }

  override def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] = {
    println("from storage, retrieving with lt: " + logicalTimestamp)

    val tmp = storage.get(implicitly[TypeTag[T]])

    tmp.flatMap(_.get(logicalTimestamp)
      .asInstanceOf[Option[(U, Option[T])]])
    /*// with for-comprehension:
    for {
      map <- storage.get(implicitly[TypeTag[T]])
      ret <- map.get(logicalTimestamp).asInstanceOf[Option[(U, Option[T])]]
    } yield ret*/
  }

}

object ImmutableStorage {
  //public factory
  def apply[U: TypeTag]() = new ImmutableStorage[U](Map())

}

object UseCase2 extends App {
  println("--now testing REALLY immutableStorage")
  val myLt = ConcreteLogicalTimestamp(2, barrierFlag = false, Some("id1"), Option.empty, Option.empty)
  //ConcreteLogicalTimestamp(1,false,Some("GreetingClient"),Some("1078643148"),Some("700963116"))
  //Map(TypeTag[Int] -> Map(ConcreteLogicalTimestamp(0,false,None,None,None) -> (2022,None)))(TypeTag[Unit],Map(ConcreteLogicalTimestamp(1,false,Some(GreetingClient),Some(-1409788869),Some(700963116)) -> (2023,Some(()))))
  var storageNew = ImmutableStorage[Int]()
  storageNew = storageNew.store[String](myLt, (2, Some("io"))) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned

  val retrieved4 = storageNew.retrieve[String](myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrieved is: " + retrieved4)

  val retrievedObj = storageNew.retrieveObject(myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrievedObject is: " + retrievedObj)

  /*
  println("--now testing mutableStorage")
  val mutableStorage = new ThreadSafeMutableStorage[Int]()
  mutableStorage.store[String](myLt, (2, Some("io"))) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  val retrieved2 = mutableStorage.retrieve[String](myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrieved is: " + retrieved2)


  val retrieved3 = mutableStorage.retrieve[Object](myLt) //type param is fundamental (if don't passed the typetag is nothing and nothig (literaly) is returned
  println("retrieved asObject is: " + retrieved3)
*/

}

