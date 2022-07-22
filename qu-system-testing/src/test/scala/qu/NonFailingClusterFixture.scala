package qu

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.service.datastructures.RemoteCounterServer

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt


trait NonFailingClusterFixture extends Matchers with AsyncMockFactory {

  self: ServersFixture =>

  var healthyCluster: LocalQuServerCluster = _

  def setupNonFailingCluster(): Future[_] = {
    healthyCluster = LocalQuServerCluster[Int](quServerIpPorts,
      keysByServer,
      thresholds,
      RemoteCounterServer.builder,
      InitialObject)
    Future {
      healthyCluster.start()
    }
  }

  def tearDownNonFailingCluster(): Future[_] = healthyCluster.shutdown()

  abstract override def withFixture(test: NoArgAsyncTest) = {
    new FutureOutcome(for {
      _ <- setupNonFailingCluster()
      result <- super.withFixture(test).toFuture
      _ <- tearDownNonFailingCluster()
    } yield result)
  }
}
