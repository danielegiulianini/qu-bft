package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Status, StatusRuntimeException}
import org.scalamock.function.MockFunction3
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Futures.{whenReady, whenReadyImpl}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.time.{Millis, Seconds, Span}
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.examples.Commands.{GetObj, Increment}
import qu.stub.client.JwtAsyncClientStub
import qu.model.ConcreteQuModel.{Request, _}
import qu.model.{ConcreteQuModel, KeysUtilities, OHSUtilities}
import qu.model.StatusCode.{FAIL, SUCCESS}

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Success
import org.scalatest._

class SimpleClientQuorumPolicySpec extends AnyFunSpec with MockFactory with ScalaFutures
  with FourServersScenario
  with OHSUtilities
  with KeysUtilities {

  val patienceConfig2 =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val mockedServersStubs: Map[String, JwtAsyncClientStub[JavaTypeable]] =
    serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val policy = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
    mockedServersStubs
  )

  def sendIncrementRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Unit, Int], JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Unit]], Future[Response[Unit]]] = sendReq[Unit, Int, Increment](mockedStub)

  def sendGetObjRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Int, Int], JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Int]], Future[Response[Int]]] = sendReq[Int, Int, GetObj[Int]](mockedStub)

  def sendReq[T, U, _ <: Operation[T, U]](mockedStub: JwtAsyncClientStub[JavaTypeable]): MockFunction3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[T]], Future[Response[T]]]
  = mockedStub.send[Request[T, U], Response[T]](_: Request[T, U])(_: JavaTypeable[Request[T, U]], _: JavaTypeable[Response[T]])



  //sends to all servers
  describe("a Simple quorum policy") {
    describe("when asked for finding a quorum") {
      describe("and receiving it") {
        it("should not ask servers any more") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce().returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
          })

          policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
        }
      }
      //behaviour testing
      it("should broadcast to all servers") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(*, *, *).returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }
      it("should broadcast to all servers the ohs and the operation passed to it at the first round") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
            .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }

      describe("while any server is not responding") {

        //continuous with fail
        it("should keep broadcasting to all servers until receiving responses with SUCCESS by all the servers") {
          val notSuccessfulGetObjResponse = Response[Int](FAIL, 1, emptyAuthenticatedRh)
          val successfulGetObjResponse = Response[Int](SUCCESS, 1, emptyAuthenticatedRh)
          val notSuccessfulIncResponse = Response[Int](FAIL, 1, emptyAuthenticatedRh)
          val successfulIncResponse = Response[Int](SUCCESS, 1, emptyAuthenticatedRh)


          def expectRequestAndReturnResponse[T, U, Z <: Operation[T, U]](mockedStub: JwtAsyncClientStub[JavaTypeable],
                                                                         req: Request[T, U],
                                                                         respo: Future[Response[T]])
          : CallHandler3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[T]], Future[Response[T]]] =
            sendReq[T, U, Z](mockedStub).expects(req, *, *).returning(respo)

          def expect2RequestAndReturnResponse[T, U, Z <: Operation[T, U]](req: Request[T, U],
                                                                          respo: Future[Response[T]]): JwtAsyncClientStub[JavaTypeable] => CallHandler3[Request[T, U], JavaTypeable[Request[T, U]], JavaTypeable[Response[T]], Future[Response[T]]] =
            (mockedStub: JwtAsyncClientStub[JavaTypeable]) => expectRequestAndReturnResponse[T, U, Z](mockedStub, req, respo)

          val expectGetObjAndReturnSuccessfulResponse =
            expect2RequestAndReturnResponse[Int, Int, GetObj[Int]](Request(Some(GetObj()), emptyOhs(serversIds.toSet)), Future.successful(successfulGetObjResponse))
          val expectGetObjAndReturnUnSuccessfulResponse =
            expect2RequestAndReturnResponse[Int, Int, GetObj[Int]](Request(Some(GetObj()), emptyOhs(serversIds.toSet)), Future.successful(notSuccessfulGetObjResponse))

          inSequence {
            mockedServersStubs.values.foreach(expectGetObjAndReturnUnSuccessfulResponse)
            mockedServersStubs.values.foreach(expectGetObjAndReturnUnSuccessfulResponse)
            mockedServersStubs.values.foreach(expectGetObjAndReturnSuccessfulResponse)
          }


          //timeout(1000.seconds), interval(500.millis)
          //val config = PatienceConfig(timeout = Span(1000, Seconds), interval = Span(1, Seconds))
          val myFut: Future[(Option[Int], Int, OHS)] = policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet))
          //perché con questo non succede nulla? e con whenready (che fa la stesa cosa si spacca??!)
          //Await.ready(myFut, Duration.fromNanos(config.timeout.totalNanos)).eitherValue.get

         /* whenReady(myFut, timeout(100.seconds), interval(500.millis)) {
            //s => s._3 should be(emptyOhs(serversIds.toSet))
            s => s._3 should be(emptyOhs(serversIds.toSet))
          }*/
          Thread.sleep(2000)

          /*
                    whenReady(myFut, timeout = config.timeout, interval= config.interval){
                      //s => s._3 should be(emptyOhs(serversIds.toSet))
                      s => s._3 should be(emptyOhs(serversIds.toSet))
                    }*/
          /*
                    // PatienceConfig =
                    val myFut: Future[(Option[Int], Int, OHS)] = policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet))
                    Await.ready(myFut, Duration.fromNanos(config.timeout.totalNanos)).eitherValue.get
                    val i = policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet))
                    whenReady[(Option[Int], Int, OHS), Unit](i, timeout(1000.seconds), interval(500.millis)) {
                      //s => s._3 should be(emptyOhs(serversIds.toSet))
                      s => ()
                    }*/

          /*val i = policy.quorum[Int](Some(GetObj[Int]()), emptyOhs(serversIds.toSet))
          whenReady(myFut) { //timeout(1000.seconds), interval(500.millis)) {
            //s => s._3 should be(emptyOhs(serversIds.toSet))
            s => s._3 should be(emptyOhs(serversIds.toSet))
          }(config, implicitly)*/
        }
      }
      /*
       it("should keep broadcasting to all servers while any server is responding with FAIL until receiving SUCCESS responses by all the servers") {
      }

              it("should keep broadcasting to all servers the updated ohs and the given operation until receiving SUCCESS responses by all of them") {
                mockedServersStubs.values.foreach(mockedStub => {
                  sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
                    .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
                })

                policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
              }

            }

            //checking values returned match servers response...
            describe("when asked for finding a quorum and getting it in a single round of communication") {
              it("return the correct answer") {

              }

              it("return the correct order") {

              }

      //must use update(increment for seeing this)
              it("return the correct ohs") {

              }
            }

            describe("when asked for finding a quorum and getting it in more than one round of communication") {
              it("return the correct answer") {

              }

              it("return the correct order") {

              }

      //must use at least one update(increment for seeing this)
              it("return the correct ohs") {

              }
            }*/

      //continuous with fail


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
              .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
          })
          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)).failed) { e =>
            e shouldBe a[StatusRuntimeException]
          }
        }
      }
    }
  }
}