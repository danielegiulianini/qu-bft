package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.RecipientInfo.id
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

trait ClusterWithFailingServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>


  var clusterWithFailingServer: LocalQuServerCluster = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    clusterWithFailingServer = LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      InitialObject)

    complete {
      clusterWithFailingServer.start()
      clusterWithFailingServer.shutdownServer(id(quServer1))

      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      clusterWithFailingServer.shutdown()
      Thread.sleep(1000)
    }
  }
}
