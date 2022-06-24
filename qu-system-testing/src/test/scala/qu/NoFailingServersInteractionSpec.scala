package qu

import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import qu.FutureUtils.seqFutures
import qu.model.OHSUtilities
import qu.model.examples.Commands.{GetObj, Increment}
import qu.service.ServersFixture

import scala.concurrent.Future

class NoFailingServersInteractionSpec extends AsyncFunSpec with Matchers
  with ServersFixture
  with OHSUtilities
  with HealthyClusterFixture
  with AuthenticatedQuClientFixture
  with AuthenticatingClientFixture
  with AuthServerFixture {

  //type information survives network transit
  describe("A Q/U protocol interaction with a quorum without failing servers") {

    describe("when a query is issued") {
      it("should return to client the expected answer value") {
        for {
          authenticatedQuClient <- quClientAsFuture
          value <- authenticatedQuClient.submit[Int](GetObj())
        } yield value should be(InitialObject)
      }
    }

    describe("when an update is issued") {
      it("should return to client the expected answer value") {
        for {
          authenticatedQuClient <- quClientAsFuture
          value <- authenticatedQuClient.submit[Unit](Increment())
        } yield value should be(()) //already out of future (no need for Future.successful(...))
      }
    }
    describe("when an update is issued followed by a query") {
      it("should return to client the updated value") {
        for {
          authenticatedQuClient <- quClientAsFuture
          _ <- authenticatedQuClient.submit[Unit](Increment())
          value <- authenticatedQuClient.submit[Int](GetObj())
        } yield value should be(InitialObject + 1)
      }
    }
    describe("when multiple updates are issued") {
      it("should return to client the correct answer") {
        val nIncrements = 3

        val operations = List.fill(nIncrements)(Increment())
        for {
          authenticatedQuClient <- quClientAsFuture
          value <- operations.foldLeft(Future.unit)((fut, operation) => fut.map(_ => authenticatedQuClient.submit[Unit](operation)))
        } yield value should be(())
      }
    }


    describe("when multiple updates are issued followed by a query") {

      it("should return to client the correct answer") {
        val nIncrements = 3
        val operations = List.fill(nIncrements)(Increment())
        for {
          authenticatedQuClient <- quClientAsFuture
          _ <- seqFutures(operations)(op => authenticatedQuClient.submit(op))
          queryResult <- authenticatedQuClient.submit(GetObj())
        } yield queryResult should be(InitialObject + nIncrements)

      }
    }
  }
}