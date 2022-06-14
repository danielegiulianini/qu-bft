package qu.storage

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Outcome, TestSuiteMixin}
import org.scalatest.matchers.should.Matchers


trait StorageFixture extends TestSuiteMixin with Matchers with MockFactory {

  var storage: ImmutableStorage[Int] = _

  abstract override def withFixture(test: NoArgTest): Outcome = {
    // Perform setup
    storage = ImmutableStorage[Int]()
    try super.withFixture(test) // To be stackable, must call super.withFixture
    //no clean up
  }
}

