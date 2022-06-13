package qu.storage

import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuiteMixin
import org.scalatest.matchers.should.Matchers
import scala.reflect.runtime.universe._


trait StorageFixture extends TestSuiteMixin with Matchers with MockFactory {
  var storage: ImmutableStorage[Int] = _

  abstract override def withFixture(test: NoArgTest) = {
    // Perform setup
    storage = ImmutableStorage[Int]()
    try super.withFixture(test) // To be stackable, must call super.withFixture
    //no clean up
  }
}

