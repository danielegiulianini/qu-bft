package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome}
import org.scalatest.matchers.should.Matchers
import qu.auth.server.AuthServer
import qu.client.QuClientImpl
import qu.service.datastructures.RemoteCounterServer
import qu.service.{AbstractServersFixture, LocalQuServerCluster, ServersFixture}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

trait AuthenticatedQuClientFixture extends AsyncTestSuiteMixin with Matchers with AsyncMockFactory {

  self: AbstractServersFixture with AuthenticatingClientFixture with AuthServerFixture =>

  var quClientAsFuture: Future[QuClientImpl[Int, JavaTypeable]] = _

  /*override def withFixture(test: NoArgAsyncTest): FutureOutcome = {

    // Perform setup
    quClientAsFuture = for {
      _ <- authClient.register()
      builder <- authClient.authorize()
    } yield builder
      .addServers(quServerIpPorts)
      .withThresholds(thresholds).build

    complete {
      super.withFixture(test) // To be stackable, must call super.withFixture
    } lastly {
      // Perform cleanup here
      quClientAsFuture.map(_.shutdown())
      //can't wait the future here (see async***spec doc)

      Thread.sleep(3000)
    }
  }*/

  // Do whatever setup you need here.
  def setupAuthenticatedQuClient(): Future[_] = {
    quClientAsFuture = for {
      _ <- Future {
        println("setting up authenticatedQuClient...")
      }
      quClientAsFuture <- for {

        _ <- Future {
          println("okok...")
        }

        _ <- authClient.register()
        _ <- Future {
          println("cient registerdd!!")
        }
        builder <- authClient.authorize()
        a <- Future {
          builder
            .addServers(quServerIpPorts)
            .withThresholds(thresholds).build
        }
        _ <- Future {
          println("client built!")
        }
      } yield a
    } yield quClientAsFuture

    quClientAsFuture
  }


  // Cleanup whatever you need here.
  def tearDownAuthenticatedQuClient(): Future[_] = for {
    _ <- Future {
      println("threadId shutting down quClient: " + Thread.currentThread)
    }
    quClient <- quClientAsFuture
    _ <- Future {
      println("shutting down policies now!")
    }
    _ <- quClient.shutdown()
  } yield ()

  abstract override def withFixture(test: NoArgAsyncTest) = {
    println("nella withFixture di AsyncFixture")
    new FutureOutcome(for {
      _ <- setupAuthenticatedQuClient()
      result <- super.withFixture(test).toFuture
      _<-Future{println("prima del teardown")}
      _ <- tearDownAuthenticatedQuClient()
    } yield result)
  }

  /*override def withFixture(test: NoArgAsyncTest) = {
    println("in fixture of AuthenticatedQuClientFixture...presente")
    new FutureOutcome(for {
      _ <- this.setup()
      result <- super.withFixture(test).toFuture
      _ <- this.tearDown()
    } yield result)
  }*/


}
