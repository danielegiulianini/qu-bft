package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessServerBuilder
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import qu.Container.{GetObj, Increment}
import qu.RecipientInfo.id
import qu.StubFactories.{inNamedProcessJacksonStubFactory, inProcessJacksonJwtStubFactory}
import qu.auth.Constants
import qu.model.ConcreteQuModel._
import qu.model.{QuorumSystemThresholds, StatusCode}
import qu.model.StatusCode.FAIL
import qu.service.AbstractQuService.{ServerInfo, jacksonSimpleQuorumServiceFactory}
import qu.service.JwtAuthorizationServerInterceptor

import scala.collection.immutable.{Map, List => RH}
import scala.concurrent.Future

object Container {
  class Increment extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  class GetObj[ObjectT] extends QueryReturningObject[ObjectT]
}


class QuServiceSpec extends AsyncFunSpec with Matchers {

  //fixture
  val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()

  val quServer1 = RecipientInfo(ip = "ciao2", port = 1)
  val quServer2 = RecipientInfo(ip = "localhost", port = 2)
  val quServer3 = RecipientInfo(ip = "localhost", port = 3)
  val quServer4 = RecipientInfo(ip = "localhost", port = 4)

  val keysByServer: Map[ServerId, Map[ServerId, Key]] = Map(
    id(quServer1) -> Map(id(quServer1) -> "ks1s1",
      id(quServer2) -> "ks1s2",
      id(quServer3) -> "ks1s3",
      id(quServer4) -> "ks1s4"),
    id(quServer2) -> Map(id(quServer1) -> "ks2s1",
      id(quServer2) -> "ks2s2",
      id(quServer3) -> "ks2s3",
      id(quServer4) -> "ks2s4"),
    id(quServer3) -> Map(id(quServer1) -> "ks3s1",
      id(quServer2) -> "ks3s2",
      id(quServer3) -> "ks3s3",
      id(quServer4) -> "ks3s4"),
    id(quServer4) -> Map(id(quServer1) -> "ks4s1",
      id(quServer2) -> "ks4s2",
      id(quServer3) -> "ks4s3",
      id(quServer4) -> "ks4s4"))

  val quServer1WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer1)))
  val quServer2WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer2)))
  val quServer3WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer3)))
  val quServer4WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer4)))

  val InitialObject = 2022
  val FaultyServersCount = 1
  val MalevolentServersCount = 0

  var service = serviceFactory(quServer1WithKey,
    InitialObject,
    QuorumSystemThresholds(t = FaultyServersCount, b = MalevolentServersCount))

  service = service.addServer(quServer2WithKey)
    .addServer(quServer3WithKey)
    .addServer(quServer4WithKey)
    .addOperationOutput[Int]()
    .addOperationOutput[Unit]()
    .addOperationOutput[Void]()


  import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => LT}

  def emptyOhsRepresentation(servers: Set[ServerId]) = Some("ohsrepr") //represent(emptyOhs(servers)))

  val aOperationRepresentation = Some("oprepr") //represent[Int, Int](Some(new GetObj[Int]())))

  val serverIds = keysByServer.keys.toSet

  val rhsWithMethodById = Map[ServerId, ReplicaHistory](
    id(quServer1) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  def rhsWithInline(barrierFlag: Boolean): Map[ServerId, ReplicaHistory] = Map[ServerId, ReplicaHistory](
    id(quServer1) -> RH(
      emptyCandidate,
      (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  val rhsWithInlineMethod: Map[ServerId, ReplicaHistory] = rhsWithInline(true)
  val rhsWithInlineBarrier: Map[ServerId, ReplicaHistory] = rhsWithInline(false)

  val rhsWithBarrier = Map[ServerId, ReplicaHistory](
    id(quServer1) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(2, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(3, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(4, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  //  override type ReplicaHistory = SortedSet[Candidate]
  //  override type α = Map[ServerId, HMAC]
  //  override type AuthenticatedReplicaHistory = (ReplicaHistory, α)
  //  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]

  def generateOhsFromRHsAndKeys(rhs: Map[ServerId, ReplicaHistory], keys: Map[ServerId, Map[ServerId, Key]]): OHS =
    keys.map { case (id, keys) => id -> (rhs(id), updateAuthenticatorFor(keys)(id)(rhs(id))) }

  val ohsWithMethod: OHS = generateOhsFromRHsAndKeys(rhsWithMethodById, keysByServer)
  val ohsWithInlineMethod: OHS = generateOhsFromRHsAndKeys(rhsWithInlineMethod, keysByServer)
  val ohsWithInlineBarrier: OHS = generateOhsFromRHsAndKeys(rhsWithInlineBarrier, keysByServer)
  val ohsWithBarrier: OHS = generateOhsFromRHsAndKeys(rhsWithBarrier, keysByServer)

  /*
    println("****rhs WITH METHOD*****")
    println(rhsWithMethodById)

    println("****rhs WITH INLINE METHOD*****")
    println(rhsWithInlineMethod)

    println("****rhs WITH INLINE BARRIER*****")
    println(rhsWithInlineBarrier)

    println("****rhs WITH BARRIER*****")
    println(rhsWithBarrier)
    /*----------------------------------*/
    println("****OHS WITH INVALID AUTHENTICATOR*****")
    //println(ohsWithInvalidAuthenticatorFor(id(quServer1)))

    println("****OHS WITH METHOD*****")
    println(ohsWithMethod)

    println("****OHS WITH INLINE METHOD*****")
    println(ohsWithInlineMethod)

    println("****OHS WITH INLINE BARRIER*****")
    println(ohsWithInlineBarrier)

    println("****OHS WITH BARRIER*****")
    println(ohsWithBarrier)

    println("****OHS WITH INVALID AUTHENTICATOR*****")
    //println(ohsWithInvalidAuthenticatorFor(id(quServer1)))*/


  def invalidateAuthenticatorForServer(serverId: ServerId): α = {
    val (_, originalAuthenticator) = ohsWithInlineMethod(serverId)
    originalAuthenticator.map {
      case (id, _) if id == serverId => id -> "corrupted"
    }
  }

  /*def ohsWithInvalidAuthenticatorFor(serverId: ServerId): OHS =
    ohsWithMethod.map { case (sid, (rh, _)) if sid == serverId => (sid, (rh, invalidateAuthenticatorForServer(sid))) }
*/

  //todo should be authenticated jacksonclientstub con canale inprocess
  val unAuthStub = inNamedProcessJacksonStubFactory(quServer1.ip, quServer1.port)

  //non c'è bisogno che faccia generare il token dall'auth service, posso generarlo io qui...
  private def getJwt: String = {
    Jwts.builder.setSubject("GreetingClient").signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
  }

  val authStub = inProcessJacksonJwtStubFactory(getJwt, quServer1.ip, quServer1.port)

  //could use QuServer construsctor too... (but it would not be in-process...)
  //so simulating here una InprocessQuServer (could reify in (fixture) class)
  val server = InProcessServerBuilder
    .forName(id(quServer1))
    .intercept(new JwtAuthorizationServerInterceptor())
    .addService(service)
    .build

  server.start()

  Thread.sleep(1000)

  //utility for more readability (not working...) (not used...)
  def sendRequest[AnswerT, ObjectT](grpcClientStub: GrpcClientStub[JavaTypeable],
                                    operation: Option[Operation[AnswerT, ObjectT]],
                                    ohs: OHS):
  Future[Response[Option[AnswerT]]] =
    grpcClientStub.send[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](Request[AnswerT, ObjectT](operation,
      ohs))

  describe("A Service") {

    describe("when contacted by an unauthenticated user") {

      it("should fail") {
        recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(new Increment()),
              ohs = emptyOhs(serverIds)))
        }
      }


      describe("when OHS contains all valid authenticators") {
        describe("and OHS is not current and the requested operation is an update") {
          val responseForUpdateWithOutdatedOhs = for {
            _ <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
              Request(operation = Some(new Increment()),
                ohs = emptyOhs(serverIds)))
            response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
              Request(operation = Some(new GetObj()),
                ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
          } yield response
          it("should fail") {
            responseForUpdateWithOutdatedOhs.map(response => assert(response.responseCode == StatusCode.FAIL))
          }
          it("should return an empty answer") {
            responseForUpdateWithOutdatedOhs.map(response => {
              println("rhe answer returned is :" + response.answer)
              assert(response.answer == Option.empty[Unit])
            })
          }
          it("should return its updated replica history") {
            for {
              firstResponse <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                Request(operation = Some(new Increment()),
                  ohs = emptyOhs(serverIds)))
              response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
                Request(operation = Some(new GetObj()),
                  ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
            } yield assert(response.authenticatedRh == firstResponse.authenticatedRh)
          }
        }
        describe("and OHS is not current and the requested operation is a query") {
          val responseForQueryWithOutdatedOhs = for {
            _ <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
              Request(operation = Some(new Increment()),
                ohs = emptyOhs(serverIds)))
            response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
              Request(operation = Some(new GetObj()),
                ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
          } yield response
          it("should fail") {
            responseForQueryWithOutdatedOhs.map(response => assert(response.responseCode == StatusCode.FAIL))
          }
          it("should return the updated answer") {
            responseForQueryWithOutdatedOhs.map(response => assert(response.answer.contains(2023)))
          }
          it("should return its updated replica history") {
            for {
              firstResponse <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                Request(operation = Some(new Increment()),
                  ohs = emptyOhs(serverIds)))
              response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
                Request(operation = Some(new GetObj()),
                  ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
            } yield assert(response.authenticatedRh == firstResponse.authenticatedRh)
          }
        }


        describe("and OHS is current") {
          describe("and OHS is classifiable as a method") {

            describe("and method is an update") {
              succeed

            }
            describe("and method is a query") {
              it("should not edit its replica history") {
                succeed
              }

            }
          }

        }


      }

      describe("when OHS contains invalid authenticator referred to its replica history") {
        /*it("should cull it") {
          sendRequest[Int, Int](myStub,
            Request(operation = Some(new GetObj[Int]()), //invio una operazione di get che non fa nulla...
              ohs = ohsWithInvalidAuthenticatorFor(id(quServer1))))
            .map(response => {
              val (rh, _) = response.authenticatedRh
              assert(rh == emptyRh)
            })
        }*/
        succeed
      }
    }





    //***BASIC FUNCTIONING***
    //barrier always (order < r) accepted

    //server culls replica

    //stall ohs triggers fail

    //authenticator computed correctly

    //object sync

    //***OPTIMIZATIONS***

    //inline repair

    //repeated requests

    //***ADVANCED FUNCTIONING***

  }
}