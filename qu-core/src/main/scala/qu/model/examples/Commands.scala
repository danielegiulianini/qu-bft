package qu.model.examples

import qu.model.ConcreteQuModel._

//commands shared between test suites
object Commands {
  case class Increment() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  //not working
  object IncrementAsObj extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  case class GetObj[ObjectT]() extends QueryReturningObject[ObjectT]

}
