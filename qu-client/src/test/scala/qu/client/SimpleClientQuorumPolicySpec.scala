package qu.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Status, StatusRuntimeException}
import org.scalamock.function.MockFunction3
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.ConcreteQuModel.{Request, _}
import qu.model.StatusCode.{FAIL, SUCCESS}
import qu.model.examples.Commands.{GetObj, Increment}
import qu.model.{FourServersScenario, KeysUtilities, OHSUtilities}
import qu.stub.client.JwtAsyncClientStub

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class SimpleClientQuorumPolicySpec extends AnyFunSpec with MockFactory with ScalaFutures
  with FourServersScenario
  with OHSUtilities
  with KeysUtilities {

  //val patienceConfig2 =PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val mockedServersStubs: Map[String, JwtAsyncClientStub[JavaTypeable]] =
    serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val policy = new JacksonSimpleBroadcastClientPolicy[Int](thresholds, mockedServersStubs)

  def sendIncrementRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Unit, Int], JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Option[Unit]]], Future[Response[Option[Unit]]]] = sendReq[Unit, Int, Increment](mockedStub)

  def sendGetObjRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Int, Int], JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Option[Int]]], Future[Response[Option[Int]]]] = sendReq[Int, Int, GetObj[Int]](mockedStub)

  def sendReq[T, U, _ <: Operation[T, U]](mockedStub: JwtAsyncClientStub[JavaTypeable]): MockFunction3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[Option[T]]], Future[Response[Option[T]]]]
  = mockedStub.send[Request[T, U], Response[Option[T]]](_: Request[T, U])(_: JavaTypeable[Request[T, U]], _: JavaTypeable[Response[Option[T]]])

  val notSuccessfulGetObjResponse = Response[Option[Int]](FAIL, Some(1), emptyAuthenticatedRh)
  val successfulGetObjResponse = Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)
  val notSuccessfulIncResponse = Response[Option[Unit]](FAIL, Some(()), emptyAuthenticatedRh)
  val successfulIncResponse = Response[Option[Unit]](SUCCESS, Some(()), emptyAuthenticatedRh)

  def expectRequestAndReturnResponse[T, U, Z <: Operation[T, U]](mockedStub: JwtAsyncClientStub[JavaTypeable],
                                                                 req: Request[T, U],
                                                                 res: Future[Response[Option[T]]])
  : CallHandler3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[Option[T]]], Future[Response[Option[T]]]] =
    sendReq[T, U, Z](mockedStub).expects(req, *, *).returning(res)

  def expect2RequestAndReturnResponse[T, U, Z <: Operation[T, U]](req: Request[T, U],
                                                                  res: Future[Response[Option[T]]]):
  JwtAsyncClientStub[JavaTypeable] => CallHandler3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[Option[T]]], Future[Response[Option[T]]]] =
    (mockedStub: JwtAsyncClientStub[JavaTypeable]) => expectRequestAndReturnResponse[T, U, Z](mockedStub, req, res)

  val expectGetObjAndReturnSuccessfulResponse =
    expect2RequestAndReturnResponse[Int, Int, GetObj[Int]](Request(Some(GetObj()),
      emptyOhs(serversIds.toSet)),
      Future.successful(successfulGetObjResponse))
  val expectGetObjAndDoNotReturn =
    expect2RequestAndReturnResponse[Int, Int, GetObj[Int]](Request(Some(GetObj()),
      emptyOhs(serversIds.toSet)),
      Future.never)

  val expectGetObjAndReturnUnSuccessfulResponse =
    expect2RequestAndReturnResponse[Int, Int, GetObj[Int]](Request(Some(GetObj()),
      emptyOhs(serversIds.toSet)),
      Future.successful(notSuccessfulGetObjResponse))

  val multiStepsScenarioTimeout = timeout(7.seconds)
  val multiStepsScenarioInterval = interval(500.millis)


  //sends to all servers
  describe("a Simple quorum policy") {
    describe("when asked for finding a quorum") {
      describe("and receiving it in a single round of communication") {
        it("should not ask servers any more") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
          })

          policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
        }
        //should return answer correctly
        it("should return the answer received") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
          })

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))) {
            //(Option[AnswerT], Int, OHS)
            _._1 should be(Some(1))
          }
        }
        //should return count correctly
        it("should return the actual count of the most voted response") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
          })

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))) {
            //(Option[AnswerT], Int, OHS)
            _._2 should be(thresholds.q)
          }
        }
        //should return ohs correctly
        it("should return the ohs correctly updated after the communications with servers") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
          })

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))) {
            //(Option[AnswerT], Int, OHS)
            _._3 should be(emptyOhs(serversIds.toSet -- serversIds.takeRight(thresholds.n - thresholds.q).toSet))
          }
        }
      }
      describe("and receiving it after n rounds of communication") {

        it("should not ask servers any more") {
          inSequence {
            mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
            mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
            mockedServersStubs.values.foreach(mockedStub => {
              sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce() //no more asking
                .returning(Future.successful(successfulGetObjResponse))
            })
          }

          whenReady(policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            _ => succeed //testing behaviour only here
          }
        }
        //should return answer correctly
        it("should return the answer received") {
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          //only q are needed to "close" the quorum
          mockedServersStubs.values.take(thresholds.q).foreach(expectGetObjAndReturnSuccessfulResponse)
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.q).foreach(expectGetObjAndDoNotReturn)

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            //(Option[AnswerT], Int, OHS)
            _._1 should be(Some(1))
          }
        }
        //should return count correctly
        it("should return the actual count of the most voted response") {
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          //only q are needed to "close" the quorum
          mockedServersStubs.values.take(thresholds.q).foreach(expectGetObjAndReturnSuccessfulResponse)
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.q).foreach(expectGetObjAndDoNotReturn)

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            //(Option[AnswerT], Int, OHS)
            _._2 should be(thresholds.q)
          }
        }
        //should return ohs correctly
        it("should return the ohs correctly updated after the communications with servers") {
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
          //only q are needed to "close" the quorum
          mockedServersStubs.values.take(thresholds.q).foreach(expectGetObjAndReturnSuccessfulResponse)
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.q).foreach(expectGetObjAndDoNotReturn)

          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            //(Option[AnswerT], Int, OHS)
            _._3 should be(emptyOhs(serversIds.toSet -- serversIds.takeRight(thresholds.n - thresholds.q).toSet))
          }
        }
      }

      //behaviour testing
      it("should broadcast to all servers") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(*, *, *).returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }

      it("should broadcast to all servers the ohs and the operation passed to it at the first round") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
            .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }


      describe("while less than q servers are responding with success") {

        //continuous with fail
        it("should keep broadcasting to all servers until receiving responses with SUCCESS by q servers") {

          inSequence {
            mockedServersStubs.values.foreach(expectGetObjAndReturnUnSuccessfulResponse)
            mockedServersStubs.values.foreach(expectGetObjAndReturnUnSuccessfulResponse)
            //only q are needed to "close" the quorum
            mockedServersStubs.values.take(thresholds.q).foreach(expectGetObjAndReturnSuccessfulResponse)
            mockedServersStubs.values.takeRight(thresholds.n - thresholds.q).foreach(expectGetObjAndDoNotReturn)
          }


          whenReady(policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            //not the ony
            _._3 should be(emptyOhs(serversIds.toSet -- serversIds.takeRight(thresholds.n - thresholds.q).toSet))
          }
        }
      }

      describe("while less than q servers are responding") {
        it("should keep broadcasting to all servers until receiving responses with SUCCESS by q servers") {

          inSequence {
            mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
            mockedServersStubs.values.foreach(expectGetObjAndDoNotReturn)
            //only q are needed to "close" the quorum
            mockedServersStubs.values.take(thresholds.q).foreach(expectGetObjAndReturnSuccessfulResponse)
            mockedServersStubs.values.takeRight(thresholds.n - thresholds.q).foreach(expectGetObjAndDoNotReturn)
          }

          whenReady(policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet)),
            multiStepsScenarioTimeout,
            multiStepsScenarioInterval) {
            //not the ony
            _._3 should be(emptyOhs(serversIds.toSet -- serversIds.takeRight(thresholds.n - thresholds.q).toSet))
          }
        }
      }

      //checking exceptions launched match servers thrown exceptions...
      describe("when asked for finding a quorum with max t faulty servers " +
        "and receiving more than t exceptions") {
        it("should throw the corresponding exception to the caller") {
          //get o inc è uguale...

          //cannot control over the order quorumPolicy stubs are called
          mockedServersStubs.values.take(thresholds.t + 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.failed(new StatusRuntimeException(Status.UNAUTHENTICATED)))
          })

          //queste non dovrebbero essere chiamate... (perché l'interceptor si ferma a t+1 e poi ritorna l'exception
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.t - 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Option[Int]](SUCCESS, Some(1), emptyAuthenticatedRh)))
          })
          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)).failed) { e =>
            e shouldBe a[StatusRuntimeException]
          }
        }
      }
    }
  }
}