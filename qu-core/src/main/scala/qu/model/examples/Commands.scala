package qu.model.examples

import qu.model.QuorumSystemThresholdQuModel._

//some examples of command definition.
object Commands {
  case class Increment() extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  object IncrementAsObj extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  case class GetObj[ObjectT]() extends QueryReturningObject[ObjectT]

}
