package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait NonFailingClusterFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>

  var healthyCluster: LocalQuServerCluster = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    healthyCluster = LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      InitialObject)

    complete {
      healthyCluster.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      healthyCluster.shutdown()
      Thread.sleep(3000)
    }
  }

}
