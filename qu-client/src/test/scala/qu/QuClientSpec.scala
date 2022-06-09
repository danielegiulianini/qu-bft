package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.function.MockFunction4
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import qu.client.{AuthenticatedQuClientImpl, BackOffPolicy, JacksonSimpleBroadcastClientPolicy, SimpleBroadcastClientPolicy}
import qu.model.examples.Commands.{GetObj, IncrementAsObj}
import qu.model.ConcreteQuModel.{Key, OHS, Operation, Request, Response, ServerId, emptyOhs}
import qu.model.examples.OHSFixture5
import qu.model.{ConcreteQuModel, QuorumSystemThresholds}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class QuClientSpec extends AnyFunSpec with MockFactory with FourServersScenario with OHSFixture5 with ScalaFutures { //AsyncFunSpec with AsyncMockFactory with OHSFixture2 {

  //devo tirar su tanti server... no basta una quorum policy
  //devo avere un altra stub per simulare contentnion

  //non ho tante leve … ho
  // 1. la answer ritornata allo user da ispezionare
  // le invocazioni a quorumrpc (numero e valore argomenti (captor) es : operation null)
  // l'invocazioni a backoff (solo numero)
  //OCCHIO CHE COME DICEVA QUELLO SPLENDIDO CONSIGLIO NON DEVO TESTARE IL GENERALE, devo stare specifico al client,
  //non devo attribuire al client errori del server ...


  //todo should go in fixture (to be shutdown between tetsts (as it is stateful))
  //stubbed dependencies
  val mockedQuorumPolicy = mock[JacksonSimpleBroadcastClientPolicy[Int]]
  val mockedBackOffPolicy = mock[BackOffPolicy]

  //using constructor (instead of builder) for wiring SUT with stubbed dependencies
  val client = new AuthenticatedQuClientImpl[Int, JavaTypeable](
    policy = mockedQuorumPolicy,
    backoffPolicy = mockedBackOffPolicy,
    serversIds = serversIds.toSet,
    thresholds = thresholds)

  val updateQuorum: MockFunction4[Option[Operation[Unit, Int]], OHS, JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Option[Unit]]], Future[(Option[Unit], Int, OHS)]] = (mockedQuorumPolicy.quorum[Unit](_: Option[Operation[Unit, Int]], _: OHS)(_: JavaTypeable[Request[Unit, Int]],
    _: JavaTypeable[Response[Option[Unit]]]))

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  describe("A client") {

    //UPDATE (inc che ritorna unit)
    describe("when requesting an update operation and receiving a response with order >= q and an ohs with method") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)
      it("should return the correct answer in a single round of communication") {
        val expectedResponse = ()

        updateQuorum.expects(Some(IncrementAsObj), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce().returning(Future.successful(
          (Some(expectedResponse), thresholds.q, ohsWithMethodFor(serversKeys))))
        (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }
      }
    }
    describe("when requesting an update operation") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)
      it("should keep asking the quorum of servers and backing off until order of the received ohs is >=q (repair)") {

        val expectedResponse = ()
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

          //(mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never().returning(Future.successful())
        }

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }

      }
    }
    //QUERY
    val queryQuorum: MockFunction4[Option[Operation[Int, Int]], OHS, JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Option[Int]]], Future[(Option[Int], Int, OHS)]] = mockedQuorumPolicy.quorum[Int](_: Option[Operation[Int, Int]], _: OHS)(_: JavaTypeable[Request[Int, Int]],
      _: JavaTypeable[Response[Option[Int]]])
    val queryOp = new GetObj[Int] //todo o uso l'bject anche qui oppure deposito in una var e uso sempre quello (essendo generico non puoi creare un object!)

    describe("when requesting a query operation and receiving a response with order >= q and an ohs with method") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)

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

        queryQuorum.expects(Some(queryOp), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce().returning(Future.successful(
          (Some(expectedResponse),
            thresholds.r /* less than q !!*/,
            ohsWithMethodFor(serversKeys))))
        (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Int](queryOp)) {
          _ should be(expectedResponse)
        }
      }
    }


    /*
    describe("when requesting an update operation 2") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)
      it("should keep asking the quorum of servers and backing off until order of the received ohs is >=q (repair)") {
        println("la ohs with inline method che pare essere method is: \n " + ohsWithInlineMethodFor(serversKeys, thresholds.r))
        val expectedResponse = ()
        val ohsWithMethod = ohsWithMethodFor(serversKeys)

        inSequence {
          updateQuorum.expects(Some(IncrementAsObj), emptyOhs(serversIdsAsSet), *, *).returning(Future.successful(
            (Some(expectedResponse), thresholds.q - 1, ohsWithInlineMethodFor(serversKeys, thresholds.r))))
          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*)

          //--possibly many of these...
          updateQuorum.expects(Option.empty, ohsWithMethod, *, *).returning(Future.successful(/*2 CONSTRAINTs: Option.empty and same ohs */
            (Some(expectedResponse), thresholds.q - 1, ohsWithInlineMethodFor(serversKeys, thresholds.r))))
          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*)
          //-------------------

          updateQuorum.expects(Option.empty, ohsWithInlineMethodFor(serversKeys, thresholds.r), *, *).returning(Future.successful(
            (Some(expectedResponse), thresholds.q, ohsWithMethod))) //ohs with method causes client to escape from repair loop

          updateQuorum.expects(Some(IncrementAsObj), ohsWithMethod, *, *).noMoreThanOnce().returning(Future.successful(
            (Some(expectedResponse), thresholds.q, ohsWithMethod)))

          (mockedBackOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()
        }

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }

      }
    }
*/

    //to be merged with upper tests
    /*describe("when repairing to deal with ohs with order < q") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)
      it("should keep asking the quorum by sending to servers the null operation") {
        succeed
      }
      it("should keep asking the quorum by sending to servers the ohs updated resulting from previous communications") {
        succeed
      }
      it("should not ask the quorum and back off after a method results from ohs") { //keep asking the quorum until a method is established"){
        succeed
      }
    }




    //QUERY
    describe("when requesting a query operation and receiving an ohs with order >= q") {
      it(" should return the correct answer in a single round of communication") {
        succeed
      }
    }
    describe("when requesting an query operation and receiving an ohs with order < q but classified as a " +
      "method (query executed optimistically)") {
      it(" should return the correct answer in a single round of communication") {
        succeed
      }
    }
    //1 o più scambi (lo scenario con più scambi generalizza)
    describe("until receiving an ohs with order <= q and without a optimistic query execution") {

      it("should continue to backoff") {

        // in sequence {
        //quorumPolicy.quorum(...).returning(OHSWithInlineMethod)
        //quorumPolicy.quorum(...)
        //backOffPolicy.backOff()
        //}
        //client.submit() . map (_== eee)

        //in sequence {
        //entro nel repair con:
        //quorumPolicy.quorum(...).returning(OHSWithInlineMethod)
        //da dentro il repair:


        // in sequence {
        //quorumPolicy.quorum(...).returning(OHSWithInlineMethod)
        //quorumPolicy.quorum(...)
        //backOffPolicy.backOff()
        //}
        //client.submit() . map (_== eee)

        // altro tipo di test
        //backOffPolicy.backOff()
        //client.submit() . map (_== eee)
        succeed
      }
      describe("when the order is less than q") {

        it("should perform a barrier") {
          /*recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(new Increment()),
              ohs = emptyOhs(serverIds)))
        }*/
          succeed
        }
      }
      describe("when the order is less than q") {

        it("should perform a barrier") {
          /*recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(new Increment()),
              ohs = emptyOhs(serverIds)))
        }*/
          succeed
        }
      }
      it("should return the correct answer") {
        /*recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(new Increment()),
              ohs = emptyOhs(serverIds)))
        }*/
        succeed
      }
    }

    describe("when receiving an ohs with order less than q but without a optimistic query execution") {

      it("should not backoff") {
        /*recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(new Increment()),
              ohs = emptyOhs(serverIds)))
        }*/
        succeed
      }
    }*/
  }
  //***BASIC FUNCTIONING***
  //client returns if order >= q (and with...)

  //client performs a barrier (object operation) if order < q

  //client (mocko la quorumPolicy) contianua ad interrogare se l'ordine è minore di x

  //client backs off if contention


  //***ADVANCED FUNCTIONING***
  //scenari di scambi più lunghi ... vedere se rispetta anche andando avanti ...


  //in fixture...
  //client.shutdown()
}
