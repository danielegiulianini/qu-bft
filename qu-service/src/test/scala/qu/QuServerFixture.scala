package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessServerBuilder
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome, Outcome, Suite, SuiteMixin}
import qu.RecipientInfo.id
import qu.auth.server.JwtAuthorizationServerInterceptor
import qu.model.ConcreteQuModel.{LogicalTimestamp, ObjectSyncResponse, latestCandidate}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.jacksonSimpleQuorumServiceFactory
import qu.service.quorum.{JacksonSimpleBroadcastServerPolicy, ServerQuorumPolicy, SimpleServerQuorumPolicy}
import qu.service.{AbstractQuService, QuServiceImpl}

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

//'withFixture(NoArgTest)' scalatest pattern (from:
// https://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest) as:
// 1. most or all tests of a suite need the same fixture
// 2. An exception in fixture code should fail the test, and not abort the suite (use a before-and-after trait instead)
trait QuServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>


  //using constructor (instead of builder) for wiring SUT with stubbed dependencies
  def freshService(): AbstractQuService[JavaTypeable, Int] = {

    val mockedQuorumPolicy = mock[JacksonSimpleBroadcastServerPolicy[Int]]

    val service = new QuServiceImpl[JavaTypeable, Int](
      methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable] {},
      policyFactory = (_, _) => mockedQuorumPolicy,
      ip = quServer1WithKey.ip,
      port = quServer1WithKey.port,
      privateKey = quServer1WithKey.keySharedWithMe,
      obj = InitialObject,
      thresholds = QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount))

    //todo could use QuServer construsctor too... (but it would not be in-process...)
    //so simulating here una InprocessQuServer (could reify in (fixture) class)
    service.addServer(quServer2WithKey)
      .addServer(quServer3WithKey)
      .addServer(quServer4WithKey)
      .addOperationOutput[Int]()
      .addOperationOutput[Unit]()
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
      server.shutdown()
      server.shutdown.awaitTermination
    }
  }
}


/* service without mocking...
val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()

var service = serviceFactory(quServer1WithKey,
  InitialObject,
  thresholds)

service = service.addServer(quServer2WithKey)
  .addServer(quServer3WithKey)
  .addServer(quServer4WithKey)
  .addOperationOutput[Int]()
  .addOperationOutput[Unit]()*/
