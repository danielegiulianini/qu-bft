import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.StatusRuntimeException
import org.scalamock.context.MockContext
import org.scalamock.function.MockFunction4
import org.scalamock.scalatest.{AsyncMockFactory, MockFactory}
import org.scalamock.util.MacroAdapter.Context
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import qu.{JwtGrpcClientStub, Shutdownable}
import qu.client.{AuthenticatedClientBuilderInFunctionalStyle, AuthenticatedQuClientImpl, BackOffPolicy, ClientQuorumPolicy, SimpleBroadcastPolicyClient}
import qu.model.Commands.{GetObj, Increment, IncrementAsObj}
import qu.model.ConcreteQuModel.{LogicalTimestamp, OHS, ObjectSyncResponse, Operation, Request, Response, ServerId, classify, emptyOhs}
import qu.model.SharedContainer.keysForServer
import qu.model.{ConcreteQuModel, OHSFixture2, QuorumSystemThresholds}

import java.util.concurrent.{Executors, ThreadPoolExecutor}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class QuClientSpec extends AnyFunSpec with MockFactory with OHSFixture2 with ScalaFutures { //AsyncFunSpec with AsyncMockFactory with OHSFixture2 {

  /*
  mocking server for verifying client
  import io.grpc.stub.StreamObserver
     @Test  def greet_messageDeliveredToServer(): Unit =  {
     val requestCaptor: Nothing = ArgumentCaptor.forClass(classOf[Nothing])
  client.greet("test name")
  verify(serviceImpl).sayHello(requestCaptor.capture, ArgumentMatchers.any[StreamObserver[Nothing]])
  assertEquals("test name", requestCaptor.getValue.getName)
  }
   */

  //c'è la questione delle fasi'
  //devo tirar su tanti server... no basta una quorum policy
  //devo avere un altra stub per simulare contentnion

  //non ho tante leve … ho
  // 1. la answer ritornata allo user da ispezionare
  // le invocazioni a quorumrpc (numero e valore argomenti (captor) es : operation null)
  // l'invocazioni a backoff (solo numero)
  //OCCHIO CHE COME DICEVA QUELLO SPLENDIDO CONSIGLIO NON DEVO TESTARE IL GENERALE, devo stare specifico al client,
  //non devo attribuire al client errori del server ...


  //deve inviare la stessa ohs ricevuta... IL VERO DUBBIO di questa suite è SE/come USARE PIù STADI O NO...

  //...al primo giro invia la ohs vuota
  // PUò DARSI CHE DURANTE LO scambio l'order cambi ??

  class JacksonSimpleBroadcastPolicyClient(private val thresholds: QuorumSystemThresholds,
                                           private val servers: Map[ServerId, JwtGrpcClientStub[JavaTypeable]])
    extends SimpleBroadcastPolicyClient[Int, JavaTypeable](thresholds, servers)


  //4-servers scenario related stuff
  val serversIds = (1 to 4 toList).map("s" + _)
  val serversIdsAsSet = serversIds.toSet
  val serversKeys: Map[ServerId, Map[ConcreteQuModel.ServerId, ServerId]] =
    serversIds.map(id => id -> keysForServer(id, serversIdsAsSet)).toMap
  val initialValue = 1
  val thresholds = QuorumSystemThresholds(t = 1, q = 3, b = 0)


  //todo should go in fixture
  //stubbed dependencies
  val mockedQuorumPolicy = mock[JacksonSimpleBroadcastPolicyClient] // mock[SimpleBroadcastPolicyClient[Int, JavaTypeable]]
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
        /*val expectedResponse = ()

        updateQuorum.expects(Some(IncrementAsObj), emptyOhs(serversIdsAsSet), *, *).noMoreThanOnce().returning(Future.successful(
          (Some(expectedResponse), thresholds.q, ohsWithMethodFor(serversKeys))))
        (backOffPolicy.backOff()(_: ExecutionContext)).expects(*).never()

        whenReady(client.submit[Unit](IncrementAsObj)) {
          _ should be(expectedResponse)
        }*/
      }
    }
    describe("when requesting an update operation") { //l'ordine può anche essere declinato in temrini più di alto livello (di concurrency...)
      it("should keep asking the quorum of servers and backing off until order of the received ohs is >=q (repair)") {
        println("la ohs with inline method che pare essere method is: \n " + ohsWithInlineMethodFor(serversKeys, thresholds.r))

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

          updateQuorum.expects(Some(IncrementAsObj), ohsWithMethod, *, *) /*.noMoreThanOnce()*/ .returning(
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
  client.shutdown()
}
