import FutureUtils.seqFutures
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import qu.ServersFixture
import qu.model.OHSUtilities
import qu.model.examples.Commands.{GetObj, Increment}

import scala.concurrent.Future

class OneFailingServerInteractionsSpec extends AsyncFunSpec with Matchers with ServersFixture with OHSUtilities
  with ClusterWithFailingServerFixture with AuthServerFixture with AuthenticatingClientFixture {

  //type information survives network transit
  describe("A Q/U protocol interaction with a quorum without failing servers") {

    lazy val quClient = for {
      _ <- client.register()
      builder <- client.authorize()
    } yield builder
      .addServers(quServerIpPorts)
      .withThresholds(thresholds).build

    describe("when a query is issued") {
      it("should return to client the expected answer value") {
        for {
          authenticatedQuClient <- quClient
          value <- authenticatedQuClient.submit[Int](GetObj())
        } yield value should be(InitialObject)
      }
    }
    describe("when an update is issued") {
      it("should return to client the expected answer value") {

        for {
          authenticatedQuClient <- quClient
          value <- authenticatedQuClient.submit[Unit](Increment())
        } yield value should be(()) //already out of future (no need for Future.successful)
      }
    }
    describe("when an update is issued followed by a query") {
      it("should return to client the updated value") {
        for {
          authenticatedQuClient <- quClient
          _ <- authenticatedQuClient.submit[Unit](Increment())
          value <- authenticatedQuClient.submit[Int](GetObj())
        } yield value should be(InitialObject + 1)
      }
    }
    describe("when multiple updates are issued") {
      it("should return to client the correct answer") {
        val operations = List.fill(3)(Increment())
        for {
          authenticatedQuClient <- quClient
          value <- operations.foldLeft(Future.unit)((fut, operation) => fut.map(_ => authenticatedQuClient.submit[Unit](operation)))
        } yield value should be(())
      }
    }


    describe("when multiple updates are issued followed by a query") {

      it("should return to client the correct answer") {
        val nIncrements = 3
        val operations = List.fill(nIncrements)(Increment())
        for {
          authenticatedQuClient <- quClient
          _ <- seqFutures(operations)(op => authenticatedQuClient.submit(op))
          queryResult <- authenticatedQuClient.submit(GetObj())
        } yield queryResult should be(InitialObject + nIncrements)

      }
    }
  }
}
