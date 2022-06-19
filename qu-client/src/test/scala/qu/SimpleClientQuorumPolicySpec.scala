package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Status, StatusRuntimeException}
import org.scalamock.function.MockFunction3
import org.scalamock.scalatest.{AsyncMockFactory, MockFactory}
import org.scalatest.concurrent.Futures.whenReady
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.must.Matchers.{be, convertToAnyMustWrapper}
import org.scalatest.matchers.should.Matchers._
import org.scalatest.matchers.should.Matchers.{a, convertToAnyShouldWrapper}
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.examples.Commands.{GetObj, Increment}
import qu.stub.client.JwtAsyncClientStub
import qu.model.ConcreteQuModel._
import qu.model.{KeysUtilities, OHSUtilities}
import qu.model.StatusCode.{FAIL, SUCCESS}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure


//not used async traits for leveraging (possibly in the future) inSequence
class SimpleClientQuorumPolicySpec extends AsyncFunSpec with AsyncMockFactory //with ScalaFutures
  with FourServersScenario
  with OHSUtilities
  with KeysUtilities {

  val mockedServersStubs: Map[String, JwtAsyncClientStub[JavaTypeable]] =
    serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  //implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val policy = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
    mockedServersStubs
  )

  def sendIncrementRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Unit, Int], JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Unit]], Future[Response[Unit]]] = (mockedStub.send[Request[Unit, Int], Response[Unit]](_: Request[Unit, Int])(_: JavaTypeable[Request[Unit, Int]], _: JavaTypeable[Response[Unit]]))

  def sendGetObjRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Int, Int], JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Int]], Future[Response[Int]]] = (mockedStub.send[Request[Int, Int], Response[Int]](_: Request[Int, Int])(_: JavaTypeable[Request[Int, Int]], _: JavaTypeable[Response[Int]]))

  //sends to all servers
  describe("a Simple quorum policy") {
    describe("when asked for finding a quorum") {
      /*describe("and receiving it") {
        it("should not ask servers any more") {
          mockedServersStubs.values.foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce().returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
          })

          policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
        }
      }*/
      //behaviour testing
      /*it("should broadcast to all servers") {
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
      }*/

      //todo solo da rifattorizzare meglio (future con lazy val)
      /*//continuous
      it("should keep broadcasting to all servers while any server is not responding until receiving SUCCESS responses by all the servers") {
        val notSuccessfulResponse = Response[Int](FAIL, 1, emptyAuthenticatedRh)
        val successfulResponse = Response[Int](SUCCESS, 1, emptyAuthenticatedRh)

        def broadcastRequestAndExpectResponse[T, U](req: Request[T, U], respo: Response[T]) = mockedStub => {
          sendGetObjRequest(mockedStub).expects(req, *, *)
            .returning(Future.successful(respo))
          val broadcastUnSuccessfulResponse = broadcastResponse[T](Response[Int](FAIL, 1, emptyAuthenticatedRh))
          val broadcastSuccessfulResponse = broadcastResponse(Response[Int](SUCCESS, 1, emptyAuthenticatedRh))


          (Response[]) SuccessfulResponse: JwtAsyncClientStub[JavaTypeable] => Unit
          = mockedStub => {
            sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
              .returning(notSuccessfulResponse)
            inSequence {
              mockedServersStubs.values.foreach[Unit](broadcastSuccessfulResponse)
              mockedServersStubs.values.foreach(broadcastSuccessfulResponse)
              mockedServersStubs.values.foreach(mockedStub => {
                sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
                  .returning(successfulResponse)
              })
            }


            policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
          }
        }

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

      //checking exceptions launched match servers thrown exceptions...
      describe("when asked for finding a quorum with max t faulty servers " +
        "and receiving more than t exceptions") {
        it("should throw the corresponding exception to the caller") {
          //get o inc è uguale...

          /*println("t is " + thresholds.t + ", n is : " + thresholds.n)
          mockedServersStubs.values.take(thresholds.t + 1).foreach(e => println("(1o step)invio a : " + e))
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.t - 1).foreach(e => println("(2o step)invio a : " + e))*/

          //cannot control over the order quorumPolicy stubs are called
          mockedServersStubs.values.take(thresholds.t + 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.failed(new StatusRuntimeException(Status.UNAUTHENTICATED))) //fsuccessful(Response[Int](SUCCESS, initialValue, emptyAuthenticatedRh)))
          })

          //queste non dovrebbero essere chiamate (=> noMoreThanOnce)... (perché l'interceptor si ferma a t+1 e poi ritorna l'exception
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.t - 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh))) //fsuccessful(Response[Int](SUCCESS, initialValue, emptyAuthenticatedRh)))
          })
          //policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
          /*whenReady(policy.quorum[Int](Some(GetObj()).failed) { e =>
            e shouldBe a[StatusRuntimeException]
          }*/
          /*ScalaFutures.whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))) { s =>
            // run assertions against the object returned in the future
          }*/
          recoverToExceptionIf[StatusRuntimeException] {
            policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
          }.map(_.getStatus.getCode must be(Status.UNAUTHENTICATED.getCode))
        }
      }
    }
  }
}