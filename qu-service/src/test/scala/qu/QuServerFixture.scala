package qu

import io.grpc.inprocess.InProcessServerBuilder
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, Outcome, Suite, SuiteMixin}
import qu.RecipientInfo.id
import qu.service.AbstractQuService.jacksonSimpleQuorumServiceFactory
import qu.service.JwtAuthorizationServerInterceptor

import java.util.concurrent.TimeUnit

//'withFixture(NoArgTest)' scalatest pattern (from:
// https://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest) as:
// 1. most or all tests of a suite need the same fixture
// 2. An exception in fixture code should fail the test, and not abort the suite (use a before-and-after trait instead)
trait QuServerFixture extends AsyncTestSuiteMixin {

  self: AsyncTestSuite with ServersFixture =>

  val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()

  var service = serviceFactory(quServer1WithKey,
    InitialObject,
    thresholds)

  service = service.addServer(quServer2WithKey)
    .addServer(quServer3WithKey)
    .addServer(quServer4WithKey)
    .addOperationOutput[Int]()
    .addOperationOutput[Unit]()

  //todo could use QuServer construsctor too... (but it would not be in-process...)
  //so simulating here una InprocessQuServer (could reify in (fixture) class)
  val server = InProcessServerBuilder
    .forName(id(quServer1))
    .intercept(new JwtAuthorizationServerInterceptor())
    .addService(service)
    .build


  abstract override def withFixture(test: NoArgAsyncTest) = {
    // Perform setup
    server.start()
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally {
      // Perform cleanup (run at end of each test)
      server.shutdown() //.append("ScalaTest is ")
      server.awaitTermination(5, TimeUnit.SECONDS)
    }
  }
}
