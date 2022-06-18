package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{Status, StatusRuntimeException}
import org.scalamock.function.MockFunction3
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Futures.whenReady
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.funspec.AnyFunSpec
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

class SimpleClientQuorumPolicySpec extends AnyFunSpec with MockFactory with ScalaFutures
  with KeysUtilities
  with OHSUtilities
  with FourServersScenario {

  val mockedServersStubs: Map[String, JwtAsyncClientStub[JavaTypeable]] =
    serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

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

      //todo solo da rifattorizzare meglio
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

          //cannot control over the order to which
          mockedServersStubs.values.take(thresholds.t + 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.failed(new StatusRuntimeException(Status.UNAUTHENTICATED))) //fsuccessful(Response[Int](SUCCESS, initialValue, emptyAuthenticatedRh)))
          })

          //queste non dovrebbero essere chiamate...
          mockedServersStubs.values.takeRight(thresholds.n - thresholds.t - 1).foreach(mockedStub => {
            sendGetObjRequest(mockedStub).expects(*, *, *).noMoreThanOnce()
              .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh))) //fsuccessful(Response[Int](SUCCESS, initialValue, emptyAuthenticatedRh)))
          })
          //policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
          whenReady(policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet)).failed) { e =>
            e shouldBe a[StatusRuntimeException]
          }
        }
      }

      //if someone not responding (or responding with fail) resending to all

      //not sending to who is already in responseset

      //not counting 2 times the same server (il 1o server invia dopo un po' di tempo e poi ne invia due)
      //il server aspetta il ri-invio e poi ne spedisce due

      //votes count ok

      //ohs edited ok

      //if server responds always with fail with it's not included in success set

      //only single round if ohs is updated (response ok) and quorum reached

      //if exceptions by more than t servers must launch exception
    }
  }
}