package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.function.MockFunction4
import org.scalamock.scalatest.{AsyncMockFactory, MockFactory}
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, TestSuite, TestSuiteMixin}
import org.scalatest.matchers.should.Matchers
import qu.client.{QuClient, QuClientImpl}
import qu.client.backoff.BackOffPolicy
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.ConcreteQuModel._
import qu.model.FourServersScenario

import scala.concurrent.Future

//'withFixture(NoArgTest)' scalatest pattern (from:
// https://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest) as:
// 1. most or all tests of a suite need the same fixture
// 2. An exception in fixture code should fail the test, and not abort the suite (use a before-and-after trait instead)
trait QuClientFixture extends TestSuiteMixin with Matchers with MockFactory {

  self: TestSuite with FourServersScenario =>

  var client: QuClient[Int, JavaTypeable] = _
  val mockedQuorumPolicy = mock[JacksonSimpleBroadcastClientPolicy[Int]]
  val mockedBackOffPolicy = mock[BackOffPolicy]
  val updateQuorum: MockFunction4[Option[Operation[Unit, Int]], OHS, JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Option[Unit]]], Future[(Option[Unit], Int, OHS)]] = mockedQuorumPolicy.quorum[Unit](_: Option[Operation[Unit, Int]], _: OHS)(_: JavaTypeable[Request[Unit, Int]],
    _: JavaTypeable[Response[Option[Unit]]])
  val queryQuorum: MockFunction4[Option[Operation[Int, Int]], OHS, JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Option[Int]]], Future[(Option[Int], Int, OHS)]] = mockedQuorumPolicy.quorum[Int](_: Option[Operation[Int, Int]], _: OHS)(_: JavaTypeable[Request[Int, Int]],
    _: JavaTypeable[Response[Option[Int]]])

  abstract override def withFixture(test: NoArgTest) = {
    // Perform setup
    println("setting up  clientFixture...")

    //using constructor (instead of builder) for wiring SUT with stubbed dependencies
    client = new QuClientImpl[Int, JavaTypeable](
      policy = mockedQuorumPolicy,
      backoffPolicy = mockedBackOffPolicy,
      serversIds = serversIds.toSet,
      thresholds = thresholds)
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally {}
  }
}
