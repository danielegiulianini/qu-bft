import io.grpc.inprocess.InProcessServerBuilder
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.RecipientInfo.id
import qu.{FourServersScenario, RecipientInfo, ServersFixture}
import qu.service.LocalQuServerCluster
import qu.service.datastructures.RemoteCounterServer

trait CLusterWithFailingServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AsyncTestSuite with ServersFixture =>

  // Perform setup
  private[this] var quServerIpPorts = Set[RecipientInfo]()
  quServerIpPorts = quServerIpPorts + quServer1
  quServerIpPorts = quServerIpPorts + quServer2
  quServerIpPorts = quServerIpPorts + quServer3
  quServerIpPorts = quServerIpPorts + quServer4

  var clusterWithFailingServer: LocalQuServerCluster = _

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    clusterWithFailingServer = LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      0)
    clusterWithFailingServer.killServer(id(quServer1))

    complete {
      clusterWithFailingServer.start()
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      clusterWithFailingServer.shutdown()
    }
  }
}
