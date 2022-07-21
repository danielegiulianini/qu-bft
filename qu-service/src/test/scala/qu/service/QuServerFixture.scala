package qu.service

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessServerBuilder
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import presentation.CachingMethodDescriptorFactory
import qu.JacksonMethodDescriptorFactory
import qu.SocketAddress.id
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService2.QuServiceBuilder
import qu.service.quorum.JacksonBroadcastBroadcastServerPolicy
import qu.storage.ImmutableStorage

import scala.concurrent.ExecutionContext
import scala.reflect.runtime.universe._


//'withFixture(NoArgTest)' scalatest pattern (from:
// https://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest) as:
// 1. most or all tests of a suite need the same fixture
// 2. An exception in fixture code should fail the test, and not abort the suite (use a before-and-after trait instead)
trait QuServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>

  val mockedQuorumPolicy: JacksonBroadcastBroadcastServerPolicy[Int] = mock[JacksonBroadcastBroadcastServerPolicy[Int]]

  def freshService(): AbstractQuService2[JavaTypeable, Int] = {

    //using constructor (instead of builder) for wiring SUT with stubbed dependencies
    val serviceBuilder = new QuServiceBuilder(
      methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable] {},
      policyFactory = (_, _) => mockedQuorumPolicy,
      ip = quServer1WithKey.ip,
      port = quServer1WithKey.port,
      privateKey = quServer1WithKey.keySharedWithMe,
      obj = InitialObject,
      thresholds = QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount),
      storage = ImmutableStorage[Int]())

    //so simulating here una InProcessQuServer (could reify in (fixture) class)
    serviceBuilder.addServer(quServer2WithKey)
      .addServer(quServer3WithKey)
      .addServer(quServer4WithKey)
      .addOperationOutput[Int]()
      .addOperationOutput[Unit]()

    serviceBuilder.build()
  }

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    val server = InProcessServerBuilder
      .forName(id(quServer1))
      .intercept(new JwtAuthorizationServerInterceptor())
      .addService(freshService())
      .build

    complete {

      server.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      server.shutdown().awaitTermination()
    }
  }
}
