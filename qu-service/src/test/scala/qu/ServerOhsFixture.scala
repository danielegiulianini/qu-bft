package qu

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome, Suite, SuiteMixin, fixture}
import qu.RecipientInfo.id
import qu.model.ConcreteQuModel.{Key, OHS, ReplicaHistory, ServerId, emptyCandidate, emptyLT, α}
import qu.model.OHSFixture5

//todo could now go together with serverFixture
trait ServerOhsFixture { self:OHSFixture5 with ServersFixture =>

}


import org.scalatest._
/*
trait Builder extends AsyncTestSuiteMixin { this: AsyncTestSuite =>

  val builder = new StringBuilder

  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    builder.append("ScalaTest is ")
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally builder.clear()
  }
}

trait Buffer extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: fixture.NoArg) = {
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally buffer.clear()
  }
}

*/
import org.scalatest._
import collection.mutable.ListBuffer
import org.scalatest._
import collection.mutable.ListBuffer
/*
import org.scalatest.flatspec.FixtureAnyFlatSpecLike//FixtureFlatSpecLike

trait Builder extends SuiteMixin with FixtureAnyFlatSpecLike { this: Suite =>

  val builder = new StringBuilder

  abstract override def withFixture(test: NoArgTest) = {
    builder.append("ScalaTest is ")
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally builder.clear()       // Shared cleanup (run at end of each test)

  }
}

trait Buffer extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest) = {
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally buffer.clear()
  }
}

class ExampleSpec extends AnyFlatSpec with Builder with Buffer {

  "Testing" should "be easy" in {
    builder.append("easy!")
    assert(builder.toString === "ScalaTest is easy!")
    assert(buffer.isEmpty)
    buffer += "sweet"
  }

  it should "be fun" in {
    builder.append("fun!")
    assert(builder.toString === "ScalaTest is fun!")
    assert(buffer.isEmpty)
    buffer += "clear"
  }
}


import org.scalatest._
import collection.mutable.ListBuffer

trait Builder extends BeforeAndAfterEach { this: Suite =>

  val builder = new StringBuilder

  override def beforeEach() {
    builder.append("ScalaTest is ")
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally builder.clear()
  }
}

trait AsyncStub extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest) = {
  try super.withFixture(test) // To be stackable, must call super.withFixture
  finally buffer.clear()
}
}*/


//prder of declaration affects initialization order!
object Prova222 extends App with ServersFixture with OHSFixture5 with ServerOhsFixture  {
println("ciao")
}
