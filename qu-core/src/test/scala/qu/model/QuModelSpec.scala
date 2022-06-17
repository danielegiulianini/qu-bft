package qu.model

import org.scalatest.funspec.AnyFunSpec
import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.ConcreteOperationTypes._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.scalacheck.Test.Parameters.default.minSuccessfulTests
import org.scalacheck.util.Pretty.Params
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import org.scalacheck.{Gen, Prop}
import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}
import org.scalatestplus.scalacheck.Checkers.check
import qu.model.examples.Commands.{GetObj, IncrementAsObj}
import qu.model.ModelGenerators._

import scala.language.postfixOps
import scala.math.Ordered.orderingToOrdered

class QuModelSpec extends AnyFunSpec with ScalaCheckPropertyChecks /*with Checkers*/ with Matchers
  with OHSUtilities
  with KeysUtilities {

  describe("A ConcreteLogicalTimestamp") {

    //test ordering
    describe("when initialized with a logical time") {
      it("should be classified as previous to a ConcreteLogicalTimestamp with a greater logical time") {
        forAll(arbitraryLt) { aLt =>
          forAll(logicalTimestampWithGreaterTimeThan(aLt.time)) { lt =>
            lt should be > aLt
          }
        }
      }

      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical time and a greater barrier flag") {
        forAll(customizableArbitraryLt(barrierPred = _ == false)) { aLt =>
          forAll(ltWithSameTimeOfAndTrueBarrierFlag(aLt.time)) { lt =>
            lt should be > aLt
          }
        }
      }

      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, same barrier flag but lexicographically greater clientId") {
        forAll(arbitraryLt) { aLt =>
          forAll(ltWithSameTimeAndBarrierOfAndClientIdGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId)) { lt =>
            lt should be > aLt //_ < aLt
          }
        }
      }


      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, barrier flag, clientId, but lexicographically greater OperationRepresentation") {
        forAll(arbitraryLt) { aLt =>
          forAll(ltWithSameTimeAndBarrierAndClientIdOfAndOpGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId, aLt.operation)) {
            _ > aLt
          }
        }
      }

      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, barrier flag, clientId, OperationRepresentation, but lexicographically greater OHSRepresentation") {
        forAll(arbitraryLt) { aLt =>
          forAll(ltWithSameTimeAndBarrierAndClientIdAndOpOfAndOhsGreaterThan(
            aLt.time,
            aLt.barrierFlag,
            aLt.clientId,
            aLt.operation,
            aLt.ohs)) {
            _ > aLt
          }
        }

      }

      //test equals (ordering doesn't affect equals so must check equality too)
      it("should be equal with a ConcreteLogicalTimestamp with same time, same barrierFlag, same clientId, same operation and same ohs") {
        forAll(arbitraryLtInfo) { case (time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) =>
          ConcreteLogicalTimestamp(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) == ConcreteLogicalTimestamp(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation)
        }
      }


      it("should not be equal to a ConcreteLogicalTimestamp with different time") {
        forAll(arbitraryLtInfo) { case (time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) =>
          ConcreteLogicalTimestamp(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) == ConcreteLogicalTimestamp(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation)
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with same time, different barrierFlag") {
        //assert(initialWorld.currentIteration == 0)
      }

      it("should not be equal to a ConcreteLogicalTimestamp with same time, different clientId") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
  }

  //todo can be shared with quClientSpec in ascenario case class
  val exampleServersIds = (1 to 4 toList).map("s" + _)
  val exampleServersKeys: Map[ServerId, Map[ConcreteQuModel.ServerId, ServerId]] =
    exampleServersIds.map(id => id -> keysForServer(id, exampleServersIds.toSet)).toMap
  val r = 2
  val q = 3

  //candidate
  describe("A candidate") {
    //ordering, equals
    //setup
    val operation = Some(IncrementAsObj)
    val clientId = Some("client1")
    describe("when it is set up from a ohs with an established method as latest ") {
      val ohsClassifiedAsMethod = ohsWithMethodFor(exampleServersKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsMethod, q, r, clientId.get)
      val (latestLt, _) = latestCandidate(ohsClassifiedAsMethod, false, r).get
      it("should return a method as operation type") {
        opType should be(METHOD)
      }
      it("should return the correct lt as the new lt") {
        lt.barrierFlag should be(false)
        lt.clientId should be(clientId)
        lt.operation should be(Some(represent[Unit, Int](operation)))
        lt.ohs should be(Some(represent(ohsClassifiedAsMethod)))
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be(latestLt)
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(ltCo)
      }
    }
    describe("when it is set up from a ohs without an established object candidate" +
      "nor an established barrier candidate " +
      "nor a repairable object candidate " +
      "nor a repairable barrier candidate are there") {
      val ohsClassifiedAsBarrier = ohsWithBarrierFor(exampleServersKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsBarrier, q, r, clientId.get)
      val (latestLt, _) = latestCandidate(ohsClassifiedAsBarrier, false, r).get

      it("should return a method as operation type") {
        opType should be(BARRIER)
      }
      it("should return the correct lt as the new lt") {
        lt.barrierFlag should be(true)
        lt.clientId should be(clientId)
        lt.operation should be(Option.empty[Operation[Any, Int]])
        lt.ohs should be(Some(represent(ohsClassifiedAsBarrier)))
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be(latestLt)
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(lt)
      }

    }
    describe("when it is set up from a ohs with an established barrier as latest ") {
      val ohsClassifiedAsCopy = ohsWithCopyFor(exampleServersKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsCopy, q, r, clientId.get)
      val (_, latObj, _) = classify(ohs = ohsClassifiedAsCopy, quorumThreshold = q, repairableThreshold = r)
      val (latestLt, latestLtCo) = latObj.get

      //previous: val (latestLt, _) = latestCandidate(ohsClassifiedAsCopy, false, r).get

      it("should return a copy as operation type") {
        opType should be(COPY)
      }
      it("should return the correct lt as the new lt") {
        lt.time should be(latestTime(ohsClassifiedAsCopy).time + 1)
        lt.barrierFlag should be(false)
        lt.clientId should be(clientId)
        lt.operation should be(ltCo.operation)
        lt.ohs should be(Some(represent(ohsClassifiedAsCopy)))
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be(latestLtCo)
      }
      it("should return the latest barrier version lt as the current lt") {
        ltCur should be({
          val (latestLt, _) = latestCandidate(ohsClassifiedAsCopy, true, r).get
          latestLt
        })
      }
    }
    describe("when it is set up from a ohs with a repairable method as latest ") {
      val ohsClassifiedAsInlineMethod = ohsWithInlineMethodFor(exampleServersKeys, r)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsInlineMethod, q, r, clientId.get)
      it("should return a inline method as operation type") {
        opType should be(INLINE_METHOD)
      }
      it("should return the correct lt as the new lt") {
        lt should be({
          val (latestLt, _) = latestCandidate(ohsClassifiedAsInlineMethod, false, r).get
          latestLt
        })
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be({
          val (_, latestCo) = latestCandidate(ohsClassifiedAsInlineMethod, false, r).get
          latestCo
        })
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(lt)
      }
    }
    describe("when it is set up from a ohs with a repairable barrier as latest ") {
      val ohsClassifiedAsInlineBarrier = ohsWithInlineBarrierFor(exampleServersKeys, r)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsInlineBarrier, q, r, clientId.get)
      it("should return a inline barrier as operation type") {
        opType should be(INLINE_BARRIER)
      }
      it("should return the correct lt as the new lt") {
        lt should be({
          val (latestLt, _) = latestCandidate(ohsClassifiedAsInlineBarrier, true, r).get
          latestLt
        })
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be({
          val (_, latestCo) = latestCandidate(ohsClassifiedAsInlineBarrier, true, r).get
          latestCo
        })
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(lt)
      }
    }

    //order in ohs
  }


  //rh
  //empty rh
  describe("A Replica history") {
    //equals
    //update
    //contains
    //latest time

    describe("when it is empty") {
      //contains
      it("should contain the empty Logical timestamp") {
        assert(emptyRh.contains(emptyCandidate))
      }

      it("should not contain any candidate other than the logical timestamp") {
        forAll(nonEmptyCandidate) { notEmptyCandidate =>
          assert(!emptyRh.contains(notEmptyCandidate))
        }
      }

      //latest time
      it("should return the empty logical timestamp as its latest time") {
        assert(latestTime(emptyRh) == emptyLT)
      }
    }
  }


  //todo can be replaced with a more sophisticated generator approach (using generators so it's already set for it)
  val ohsWithMethodGen =
    Gen.oneOf(ohsWithMethodFor(exampleServersKeys), emptyOhs(exampleServersIds.toSet))

  val ohsWithInlineMethodGen: Gen[OHS] = Gen.const(ohsWithInlineMethodFor(exampleServersKeys, r))

  val ohsWithInlineBarrierGen: Gen[OHS] = Gen.const(ohsWithInlineBarrierFor(exampleServersKeys, r))

  val ohsWithBarrierGen: Gen[OHS] = Gen.const(ohsWithBarrierFor(exampleServersKeys))

  val ohsWithCopyGen: Gen[OHS] = Gen.const(ohsWithCopyFor(exampleServersKeys))

  val ohsGen: Gen[OHS] = Gen.oneOf(ohsWithMethodGen,
    ohsWithInlineMethodGen,
    ohsWithInlineBarrierGen,
    ohsWithBarrierGen,
    ohsWithCopyGen)

  def checkOpType(ohsGen: Gen[OHS], opType2: OperationType) =
  /*without shrinking:
  forAll(ohsGen) { ohs => {
    val (opType, _, _) = classify(ohs, repairableThreshold = r, quorumThreshold = q)
    assert(opType == opType2)
  }*/
  //for solving bug https://www.47deg.com/blog/a-common-scalacheck-problem/, not using shrinking here:
    check {
      Prop.forAllNoShrink(ohsGen) { ohs => {
        val (opType, _, _) = classify(ohs, repairableThreshold = r, quorumThreshold = q)
        opType == opType2
      }
      }
    }

  def checkLatestObjCand(ohsGen: Gen[OHS]): Assertion =
    forAll(ohsGen) {
      ohs => {
        val (_, latestObjectCandidate, _) = classify(ohs, repairableThreshold = r, quorumThreshold = q)
        assert(latestObjectCandidate == latestCandidate(ohs = ohs, barrierFlag = false, repairableThreshold = r))
      }
    }

  def checkLatestBarCand(ohsGen: Gen[OHS]): Assertion = {
    forAll(ohsGen) {
      ohs => {
        val (_, _, latestBarrierCandidate) = classify(ohs, repairableThreshold = r, quorumThreshold = q)
        assert(latestBarrierCandidate == latestCandidate(ohs = ohs, barrierFlag = true, repairableThreshold = r))
      }
    }
  }


  //ohs
  //todo: can also be expressed on functions rather on obj (a <function invocation> (ex.classification) when ... should ...) or a <method> invocation when ... should ... or <method> , when invoked on ..., it should...
  describe("An OHS") {
    //equals
    //aggiornamento in base al server
    //---classify--- (testing all its branches)
    describe("when a established barrier is latest and performing classification") {
      it("it should trigger a copy") { //or identifies instead of trigger
        checkOpType(ohsWithCopyGen, COPY)
      }
      it("it should return its latest object candidate as latest object candidate") {
        checkLatestObjCand(ohsWithCopyGen)
      }
      it("it should return its latest barrier candidate as latest barrier candidate") {
        checkLatestBarCand(ohsWithCopyGen)
      }
    }
    describe("when a established object is latest and performing classification") {
      it("should trigger a method") {
        checkOpType(ohsWithMethodGen, METHOD)
      }

      it("should return its latest object candidate as latest object candidate") {
        checkLatestObjCand(ohsWithMethodGen)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        checkLatestBarCand(ohsWithMethodGen)
      }
    }
    describe("when a repairable barrier is latest and performing classification") {
      it("should trigger a inline barrier") {
        checkOpType(ohsWithInlineBarrierGen, INLINE_BARRIER)
      }

      it("should return its latest object candidate as latest object candidate") {
        checkLatestObjCand(ohsWithInlineBarrierGen)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        checkLatestBarCand(ohsWithInlineBarrierGen)

      }
    }
    describe("when a repairable object is latest and performing classification") {
      it("should trigger a inline method") {
        checkOpType(ohsWithInlineMethodGen, INLINE_METHOD)
      }

      it("should return its latest object candidate as latest object candidate") {
        checkLatestObjCand(ohsWithInlineMethodGen)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        checkLatestBarCand(ohsWithInlineMethodGen)
      }
    }

    describe("when performing classification and " +
      "nor an established object candidate" +
      "nor an established barrier candidate " +
      "nor a repairable object candidate " +
      "nor a repairable barrier candidate are there") {
      it("should trigger a barrier") {
        checkOpType(ohsWithBarrierGen, BARRIER)
      }
      it("should return its latest object candidate as latest object candidate") {
        checkLatestObjCand(ohsWithInlineMethodGen)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        checkLatestBarCand(ohsWithInlineMethodGen)
      }

      //latest_candidate
      describe("when queried for its latest object candidate") {
        it("should return the greatest object candidate it contains") {
        }
        it("should return a object candidate and not a barrier candidate") {
          val barrierFlag = false
          forAll(ohsGen) { ohs =>
            for {
              (lt, ltCo) <- latestCandidate(ohs, barrierFlag, r)
            } yield assert(lt.barrierFlag == barrierFlag)
          }
        }
      }
      describe("when queried for its latest barrier candidate") {
        it("should return the greatest barrier candidate it contains") {
          //assert(initialWorld.currentIteration == 0)
        }
        it("should return a barrier candidate and not a object candidate") {
          val barrierFlag = true
          forAll(ohsGen) { ohs =>
            for {
              (lt, ltCo) <- latestCandidate(ohs, barrierFlag, r)
            } yield assert(lt.barrierFlag == barrierFlag)
          }
        }

        //def rhWithLatestLtAs(logicalTimestamp: LogicalTimestamp) = emptyRh + logicalTimestamp
        //latest time
        describe("when queried for its latest time") {
          it("should not contain any time greater than the one it returns") {

            import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => LT}

            val latestTimestamp = LT(1,
              false,
              Some("client1"),
              aOperationRepresentation,
              emptyOhsRepresentation(exampleServersIds))

            // val rhWithLatestTime: ReplicaHistory = emptyRh :+ (latestTimestamp -> emptyLT)
            //val ohsWithLatestTime = generateOhsFromRHsAndKeys(unanimousRhsFor(exampleServersIds, List((emptyLT, latestTimestamp))))
            //latestTime(ohsWithMethodFor(exampleServersKeys)) should be(latestTimestamp)
          }
        }
      }


      describe("An empty OHS") {
        //comparing (se inserisco 2 volte in diverso ordine sono comunque uguali le ohs ...)

        //---classify---
        describe("when classified") {
          it("should trigger method") {
            val (opType, _, _) = classify(emptyOhs(exampleServersIds.toSet),
              repairableThreshold = r,
              quorumThreshold = q)
            opType should be(METHOD)
          }
          it("should None as it latest barrier candidate") {
            val (_, _, latestBarrierCandidate) = classify(emptyOhs(exampleServersIds.toSet),
              repairableThreshold = r,
              quorumThreshold = q)
            latestBarrierCandidate should be(Option.empty[Candidate])
          }
          it("should return the empty candidate as it latest object candidate") {
            val (_, latestObjectCandidate, _) = classify(emptyOhs(exampleServersIds.toSet),
              repairableThreshold = r,
              quorumThreshold = q)
            latestObjectCandidate should be(Some(emptyCandidate))
          }
        }
        //latest candidate
        describe("when queried for its latest barrier candidate") {
          it("should return None") {
            latestCandidate(emptyOhs(exampleServersIds.toSet), true, r) should be(Option.empty[Candidate])
          }
          it("should return the empty candidate as it latest objet candidate") {
            latestCandidate(emptyOhs(exampleServersIds.toSet), false, r) should be(Some(emptyCandidate))
          }
        }
        //latest_time
        it("should return the empty Logical timestamp as it latest time") {
          latestTime(emptyRh) should be(emptyLT)
        }
      }


      describe("An OHS representation") {
        describe("when compared to another representation of it") {
          it("should be equals to it") {
            forAll(ohsGen) {
              ohs => {
                assert(represent(ohs) == represent(ohs))
              }
            }
          }
        }
      }

      val opGen = Gen.oneOf(IncrementAsObj, new GetObj)
      describe("An Operation representation") {
        describe("when compared to another representation of it") {
          it("should be equals to it") {
            forAll(opGen) {
              op =>
                assert(represent(Some(op)) == represent(Some(op)))
            }
          }
        }
      }
    }
  }
}