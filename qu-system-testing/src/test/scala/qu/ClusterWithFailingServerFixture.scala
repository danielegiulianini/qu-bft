package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.SocketAddress.id
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait ClusterWithFailingServerFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: OneFailingServersInfoFixture =>

  var clusterWithFailingServer: LocalQuServerCluster = _

  def setupClusterWithFailingServer(): Future[_] = {
    clusterWithFailingServer = LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      InitialObject)

    for {
      _ <- Future {
        clusterWithFailingServer.start()
      }
      _ <- clusterWithFailingServer.shutdownServer(id(quServer1))
    } yield ()
  }

  def tearDownClusterWithFailingServer(): Future[_] =clusterWithFailingServer.shutdown()

  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    new FutureOutcome(for {
      _ <- setupClusterWithFailingServer()
      result <- super.withFixture(test).toFuture
      _ <- tearDownClusterWithFailingServer()
    } yield result)
  }

}
