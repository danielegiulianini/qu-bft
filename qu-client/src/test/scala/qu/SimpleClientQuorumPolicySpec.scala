package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.function.{MockFunction3}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.examples.Commands.{GetObj, Increment}
import qu.model.examples.OHSFixture
import qu.stub.client.JwtAsyncClientStub
import qu.model.ConcreteQuModel._
import qu.model.StatusCode.SUCCESS

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class SimpleClientQuorumPolicySpec extends AnyFunSpec with MockFactory with FourServersScenario with OHSFixture {

  val mockedServersStubs: Map[String, JwtAsyncClientStub[JavaTypeable]] =
    serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val policy = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
    mockedServersStubs
  )

  //1. def send[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):Future[OutputT]
  //    a. ed invio la request contennte request:
  //      case class Request[ReturnValueT, ObjectT](operation: Option[Operation[ReturnValueT, ObjectT]],
  //                                            ohs: OHS)
  //    b. e mi aspetto che ritorni la response:
  //      case class Response[ReturnValueT](responseCode: StatusCode,
  //                                    answer: ReturnValueT,
  //                                    authenticatedRh: AuthenticatedReplicaHistory)

  //2. def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
  //                               ohs: OHS)
  //                              (implicit
  //                               transportableRequest: Transportable[Request[AnswerT, ObjectT]],
  //                               transportableResponse: Transportable[Response[Option[AnswerT]]])
  // : Future[(Option[AnswerT], Int, OHS)] = {
  def sendIncrementRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Unit, Int], JavaTypeable[Request[Unit, Int]], JavaTypeable[Response[Unit]], Future[Response[Unit]]] = (mockedStub.send[Request[Unit, Int], Response[Unit]](_: Request[Unit, Int])(_: JavaTypeable[Request[Unit, Int]], _: JavaTypeable[Response[Unit]]))

  def sendGetObjRequest(mockedStub: JwtAsyncClientStub[JavaTypeable]):
  MockFunction3[Request[Int, Int], JavaTypeable[Request[Int, Int]], JavaTypeable[Response[Int]], Future[Response[Int]]] = (mockedStub.send[Request[Int, Int], Response[Int]](_: Request[Int, Int])(_: JavaTypeable[Request[Int, Int]], _: JavaTypeable[Response[Int]]))


  //sends to all servers
  describe("a Simple quorum policy") {
    describe("when asked for finding a quorum") {

      //behaviour testing
      it("should broadcast to all servers") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(*, *, *).returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }
      it("should broadcast to all servers the ohs and the operation passed to it") {
        mockedServersStubs.values.foreach(mockedStub => {
          sendGetObjRequest(mockedStub).expects(Request[Int, Int](Some(GetObj()), emptyOhs(serversIds.toSet)), *, *)
            .returning(Future.successful(Response[Int](SUCCESS, 1, emptyAuthenticatedRh)))
        })

        policy.quorum[Int](Some(GetObj()), emptyOhs(serversIds.toSet))
      }
    }

    //checking values returned...



    //if someone not responding (or responding with fail) resending to all

    //sending to already obtained responses

    //not counting 2 times the same server (il 1o server invia dopo un po' di tempo e poi ne invia due)
    //il server aspetta il ri-invio e poi ne spedisce due

    //votes count ok

    //ohs edited ok

    //if server responds always with fail with it's not included in success set

    //only single round if ohs is updated (response ok) and quorum reached

    //if exceptions by more than t servers must launch exception
  }
}