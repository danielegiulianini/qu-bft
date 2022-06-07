package qu.model

import ConcreteQuModel._

object Commands {
  case class Increment() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  object IncrementAsObj extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

 case class GetObj[ObjectT]() extends QueryReturningObject[ObjectT]

}
