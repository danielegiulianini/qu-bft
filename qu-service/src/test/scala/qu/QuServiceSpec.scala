package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessServerBuilder
import org.scalamock.scalatest.{AsyncMockFactory, MockFactory}
import org.scalatest.FutureOutcome
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers
import qu.RecipientInfo.id
import qu.model.Commands.{GetObj, Increment, IncrementAsObj}
import qu.model.ConcreteQuModel._
import qu.model.{OHSFixture, QuorumSystemThresholds, StatusCode}
import qu.model.StatusCode.{FAIL, SUCCESS}
import qu.service.{AbstractQuService, JwtAuthorizationServerInterceptor, QuServiceImpl, ServerQuorumPolicy}

import scala.concurrent.Future


//since Async(FunSpec) is used Async(MockFactory) must be used (see https://scalamock.org/user-guide/integration/)
class QuServiceSpec extends AsyncFunSpec with Matchers with AsyncMockFactory
  with OHSFixture with ServersFixture with QuServerFixture with AuthStubFixture with UnAuthStubFixture {

  //for client stub fixture
  override protected val serverInfo: RecipientInfo = RecipientInfo(quServer1.ip, quServer1.port)
  override protected val clientId: String = "client1"

  describe("A Service") {

    describe("when contacted by an unauthenticated user") {

      /*it("should fail") {
        recoverToSucceededIf[StatusRuntimeException] {
          unAuthStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(IncrementAsObj),
              ohs = emptyOhs(serverIds)))
        }
      }
      describe("when OHS contains all valid authenticators") {
        //laziness needed for correct initialization dependency and for performance reason
        lazy val responseForUpdateWithOutdatedOhs = for {
          _ <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(IncrementAsObj),
              ohs = emptyOhs(serverIds)))
          response <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
            Request(operation = Some(IncrementAsObj), //sending an UPDATE operation
              ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
        } yield response
        describe("and OHS is not current and the requested operation is an update") {
          it("should fail") {
            responseForUpdateWithOutdatedOhs.map(response => assert(response.responseCode == StatusCode.FAIL))
          }
          it("should return an empty answer") {
            responseForUpdateWithOutdatedOhs.map(response => {
              assert(response.answer == Option.empty[Unit])
            })
          }
          it("should return its updated replica history but without updating it again") {
            for {
              firstResponse <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                Request(operation = Some(IncrementAsObj),
                  ohs = emptyOhs(serverIds)))
              response <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                Request(operation = Some(IncrementAsObj), //now sending an UPDATE op
                  ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
            } yield assert(response.authenticatedRh == firstResponse.authenticatedRh)
          }
        }
        /*
            describe("and OHS is not current and the requested operation is a query") {
              val responseForQueryWithOutdatedOhs = for {
                _ <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                  Request(operation = Some(new Increment()),
                    ohs = emptyOhs(serverIds)))
                response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
                  Request(operation = Some(new GetObj()), //sending a QUERY operation
                    ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
              } yield response
              it("should fail") {
                responseForQueryWithOutdatedOhs.map(response => assert(response.responseCode == StatusCode.FAIL))
              }
              it("should return the updated answer (optimistic query execution)") {
                responseForQueryWithOutdatedOhs.map(response => assert(response.answer.contains(2023)))
              }
              it("should return its updated replica history (optimistic query execution)") {
                for {
                  firstResponse <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                    Request(operation = Some(new Increment()),
                      ohs = emptyOhs(serverIds)))
                  response <- authStub.send[Request[Int, Int], Response[Option[Int]]](
                    Request(operation = Some(new GetObj()),
                      ohs = emptyOhs(serverIds))) //resending empty (outdated) ohs
                } yield assert(response.authenticatedRh == firstResponse.authenticatedRh)
              }
            }*/

        //testing all the branches of service impl
        describe("and OHS is current") {
          describe("and OHS is not classifiable as a a barrier") {
            describe("and object version is not stored at the contacted server side") {
              it("should object sync") {
                //potrei simulare uno scambio (anziché one shot scenario) e verificare che continua a fare object sync sinché chiedo oggetto che non ha...

                println("il latest candidate is : " + latestCandidate(aOhsWithMethod, barrierFlag = false, thresholds.r))
                val (_, ltCo) = latestCandidate(aOhsWithMethod, false, thresholds.r).get
                (mockedQuorumPolicy.objectSync(_: LogicalTimestamp)(_: JavaTypeable[LogicalTimestamp],
                  _: JavaTypeable[ObjectSyncResponse[Int]]))
                  .expects(/*ltCo*/ * /* * */ , *, *).returning(Future.successful(InitialObject + 1)) //per sicurezza ritorno il valore giusto

                for {
                  response <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                    Request(operation = Some(IncrementAsObj),
                      ohs = aOhsWithMethod)) //... sending ohs with obj versions that servers doesn't have to trigger sync
                } yield response.responseCode should be(SUCCESS)
              }
            }
          }*/
      describe("if ok") {
        describe("if ok") {
          describe("and OHS is classifiable as a method") {
            val ohs = emptyOhs(serverIds)
            val op = Some(IncrementAsObj)
            lazy val responseForUpdate = for {
              response <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
                Request(operation = op,
                  ohs = ohs))
            } yield response
            describe("and operation class is update") {

              println("setup cliente ***************************")
              val (_, (lt, ltCo), _) = setup(op, ohs, thresholds.q, thresholds.r, clientId)
              val correctRh = ohs(id(quServer1))._1.appended(lt -> ltCo)

              describe("and conditioned-on object is stored at service side") {
                //it should not trigger object sync
              }
              it("should edit its replica history correctly") {
                responseForUpdate.map(_.authenticatedRh._1 should be(correctRh))
              }
              it("should update server authenticators correctly") {
                println("at client side my keys are: " + keysByServer(id(quServer1)))
                val updatedAuthenticator = authenticateRh(correctRh, keysByServer(id(quServer1)))
                responseForUpdate.map(_.authenticatedRh._2 should be(updatedAuthenticator))
              }
              //should return correct answer
              it("should return success") {
                for {
                  response <- responseForUpdate
                } yield response.responseCode should be(SUCCESS)
              }
            }
            describe("and operation class is query") {
              it("should not edit its replica history") {
                fail("still to impl")
              }
              describe("and conditioned-on object is stored at service side") {
                //it should not trigger object sync

              }
              it("should succeed") {
                fail("still to impl")
              }
              //should return correct answer

            }
          }
          describe("and OHS is classifiable as a inline method") {
            //copy the ones of method (query vs update)
          }
          describe("and OHS is classifiable as a copy") {
            describe("and conditioned-on object is stored at service side") {
              //it should not trigger object sync
            }
            //update rh correctly
            it("should copy latest (lt,ltCo) forward") {
              fail("still to impl")
            }
          }
          describe("and OHS is classifiable as a inline barrier") {
            //it should not ever trigger object sync
            it("should copy latest (lt,ltCo) forward") {
              fail("still to impl")
            }
            //todo

          }
          describe("and OHS is classifiable as a barrier") {
            //it should not ever trigger object sync
            //update rh correctly:
            it("should always be accepted ") {
              fail("still to impl") //even if in contention
            }
          }
        }
      }

      /* describe("when OHS contains invalid authenticator referred to its replica history") {
         it("should cull it") {
           for {
             _ <- authStub.send[Request[Unit, Int], Response[Option[Unit]]](
               Request(operation = Some(IncrementAsObj), //updating server rh (since with emptyLt I cannot detect...)
                 ohs = ohsWithInvalidAuthenticatorFor(aOhsWithMethod, id(quServer1))))
             response <- authStub.send[Request[Int, Int], Response[Option[Unit]]](
               Request(operation = Some(new GetObj[Int]),
                 ohs = ohsWithInvalidAuthenticatorFor(aOhsWithMethod, id(quServer1))))
           } yield assert(response.responseCode == FAIL) //getting fail since because of culling client ohs become the emptyOhs (all are invalidated!)
         }
       }*/
    }
  }


}


/*  //utility for more readability (not working...) (not used...)
  def sendRequest[AnswerT, ObjectT](grpcClientStub: GrpcClientStub[JavaTypeable],
                                    operation: Option[Operation[AnswerT, ObjectT]],
                                    ohs: OHS):
  Future[Response[Option[AnswerT]]] =
    grpcClientStub.send[Request[AnswerT, ObjectT], Response[Option[AnswerT]]](Request[AnswerT, ObjectT](operation,
      ohs))*/

/*
  override type ReplicaHistory = List[Candidate] //SortedSet[Candidate] for avoid hitting bug https://github.com/FasterXML/jackson-module-scala/issues/410
  type HMAC
  override type α = Map[ServerId, HMAC]
  type AuthenticatedReplicaHistory = (ReplicaHistory, α)
  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]
 */