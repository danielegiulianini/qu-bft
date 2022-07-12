package qu.client

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import qu.model.ConcreteQuModel.emptyOhs
import qu.model.examples.Commands.{GetObj, IncrementAsObj}
import qu.model.{FourServersScenario, KeysUtilities, OHSUtilities}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.language.postfixOps

class QuClientSpec extends AnyFunSpec
  with MockFactory
  with FourServersScenario
  with KeysUtilities
  with OHSUtilities
  with ScalaFutures
  with QuClientFixture {

  //determinism in tests
  implicit val exec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  describe("A client") {

    //UPDATE
    describe("when requesting an update operation and receiving a response with order >= q and an ohs with method") {
      it("should return the correct answer in a single round of communication") {
        val expectedResponse: Unit = ()

        updateQuorum.expects(Some(IncrementAsObj), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce().returning(Future.successful(
          (Some(expectedResponse), thresholds.q, ohsWithMethodFor(serversKeys))))
        (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }
      }
    }
    describe("when requesting an update operation") {
      it("should keep asking the quorum of servers and backing off until order of the received ohs is >=q (repair)") {

        val expectedResponse: Unit = ()
        val ohsWithMethod = ohsWithMethodFor(serversKeys)
        val ohsWithInlineMethod = ohsWithInlineMethodFor(serversKeys, thresholds.r)

        inSequence {
          updateQuorum.expects(Some(IncrementAsObj), emptyOhs(serversIdsAsSet), *, *).returning(Future.successful(
            (Some(expectedResponse),
              thresholds.q - 1, //less than q
              ohsWithInlineMethod)))

          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).returning(Future.successful()) //must return a value (null pointer ex instead)!

          //--possibly many of these...
          updateQuorum.expects(Option.empty, ohsWithInlineMethodFor(serversKeys, thresholds.r), *, *).returning(
            Future.successful(
              /*2 CONSTRAINTs: Option.empty and send ohs received before*/
              (Some(expectedResponse),
                thresholds.q - 1, //less than q
                ohsWithInlineMethod)))

          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).returning(Future.successful())
          //-------------------

          updateQuorum.expects(Option.empty, ohsWithInlineMethodFor(serversKeys, thresholds.r), *, *).returning(
            Future.successful(
              (Some(expectedResponse),
                thresholds.q - 1, //not important this
                ohsWithMethod))) //ohs with method causes client to escape from repair loop

          updateQuorum.expects(Some(IncrementAsObj), ohsWithMethod, *, *).noMoreThanOnce().returning(
            Future.successful(
              (Some(expectedResponse),
                thresholds.q, //order q makes it escape from loop
                ohsWithMethod))) //not important this

          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never().returning(Future.successful())
        }

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }

      }
    }

    //QUERY
    val queryOp = GetObj[Int]()

    describe("when requesting a query operation and receiving a response with order >= q " +
      "and an ohs with method") {

      it("should return the correct answer in a single round of communication") {
        val expectedResponse = initialValue + 1

        queryQuorum.expects(Some(queryOp), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce().returning(Future.successful(
          (Some(expectedResponse), thresholds.q, ohsWithMethodFor(serversKeys))))
        (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Int](queryOp)) {
          _ should be(expectedResponse)
        }
      }
    }

    describe("when requesting an query operation a response with order < q but classified as a " +
      "method (query executed optimistically)") {

      it(" should return the correct answer in a single round of communication") {
        val expectedResponse = initialValue + 1

        queryQuorum.expects(Some(queryOp), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce()
          .returning(Future.successful((Some(expectedResponse),
            thresholds.r /* less than q !!*/ ,
            ohsWithMethodFor(serversKeys))))

        (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Int](queryOp)) {
          _ should be(expectedResponse)
        }
      }
    }
  }
}

