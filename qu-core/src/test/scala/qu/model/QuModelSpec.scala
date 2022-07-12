package qu.model

import org.scalatest.funspec.AnyFunSpec
import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => Lt}

import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.ConcreteOperationTypes._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import org.scalacheck.{Gen, Prop}
import org.scalatest.Assertion
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}
import org.scalatestplus.scalacheck.Checkers.check
import qu.model.examples.Commands.{GetObj, IncrementAsObj}

import scala.language.postfixOps
import scala.math.Ordered.orderingToOrdered

class QuModelSpec extends AnyFunSpec with ScalaCheckPropertyChecks /*with Checkers*/ with Matchers
  with OHSUtilities
  with KeysUtilities
  with FourServersScenario
  with OhsGenerators
  with ModelGenerators {

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
            lt should be > aLt
          }
        }
      }


      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, barrier flag, clientId, but lexicographically greater OperationRepresentation") {
        forAll(arbitraryLt) { aLt =>
          forAll(ltWithSameTimeAndBarrierAndClientIdOfAndOpGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId, aLt.operation)) { lt =>
            lt should be > aLt
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
            aLt.ohs)) { lt =>
            lt should be > aLt
          }
        }

      }

      //test equals (ordering doesn't affect equals so must check equality too)
      it("should be equal with a ConcreteLogicalTimestamp with same time, same barrierFlag, same clientId, same operation and same ohs") {
        forAll(arbitraryLtInfo) { case (time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) =>
          Lt(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation) should be(Lt(time, barrierFlag, clientId, operationRepresentation, ohsRepresentation))
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with all same fields but different time") {
        forAll(arbitraryLt) { lt =>
          forAll(sameLtForAllButTime(lt)) {
            _ should not be lt
          }
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with all same fields but different barrier flag") {
        forAll(arbitraryLt) { lt =>
          forAll(sameLtForAllButBarrierFlag(lt)) {
            _ should not be lt
          }
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with all same fields but different client id") {
        forAll(arbitraryLt) { lt =>
          forAll(sameLtForAllButClientId(lt)) {
            _ should not be lt
          }
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with all same fields but different operation") {
        forAll(arbitraryLt) { lt =>
          forAll(sameLtForAllButOpRepr(lt)) {
            _ should not be lt
          }
        }
      }

      it("should not be equal to a ConcreteLogicalTimestamp with all same fields but different ohs") {
        forAll(arbitraryLt) { lt =>
          forAll(sameLtForAllButOhsRepr(lt)) {
            _ should not be lt
          }
        }
      }
    }
  }

  //candidate
  describe("A candidate") {
    //ordering, equals
    //setup
    val operation = Some(IncrementAsObj)
    val clientId = Some("client1")
    describe("when it is set up from a ohs with an established method as latest ") {
      val ohsClassifiedAsMethod = ohsWithMethodFor(serversKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsMethod, thresholds.q, thresholds.r, clientId.get)
      val (latestLt, _) = latestCandidate(ohsClassifiedAsMethod, barrierFlag = false, thresholds.r).get
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

      val ohsClassifiedAsBarrier = ohsWithBarrierFor(serversKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsBarrier, thresholds.q, thresholds.r, clientId.get)
      val (latestLt, _) = latestCandidate(ohsClassifiedAsBarrier, barrierFlag = false, thresholds.r).get

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
      val ohsClassifiedAsCopy = ohsWithCopyFor(serversKeys)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsCopy, thresholds.q, thresholds.r, clientId.get)
      val (_, latObj, _) = classify(ohs = ohsClassifiedAsCopy, quorumThreshold = thresholds.q, repairableThreshold = thresholds.r)
      val (_, latestLtCo) = latObj.get

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
          val (latestLt, _) = latestCandidate(ohsClassifiedAsCopy, barrierFlag = true, thresholds.r).get
          latestLt
        })
      }
    }
    describe("when it is set up from a ohs with a repairable method as latest ") {
      val ohsClassifiedAsInlineMethod = ohsWithInlineMethodFor(serversKeys, thresholds.r)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsInlineMethod, thresholds.q, thresholds.r, clientId.get)
      it("should return a inline method as operation type") {
        opType should be(INLINE_METHOD)
      }
      it("should return the correct lt as the new lt") {
        lt should be({
          val (latestLt, _) = latestCandidate(ohsClassifiedAsInlineMethod, barrierFlag = false, thresholds.r).get
          latestLt
        })
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be({
          val (_, latestCo) = latestCandidate(ohsClassifiedAsInlineMethod, barrierFlag = false, thresholds.r).get
          latestCo
        })
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(lt)
      }
    }
    describe("when it is set up from a ohs with a repairable barrier as latest ") {
      val ohsClassifiedAsInlineBarrier = ohsWithInlineBarrierFor(serversKeys, thresholds.r)
      val (opType, (lt, ltCo), ltCur) = setup(operation, ohsClassifiedAsInlineBarrier, thresholds.q, thresholds.r, clientId.get)
      it("should return a inline barrier as operation type") {
        opType should be(INLINE_BARRIER)
      }
      it("should return the correct lt as the new lt") {
        lt should be({
          val (latestLt, _) = latestCandidate(ohsClassifiedAsInlineBarrier, barrierFlag = true, thresholds.r).get
          latestLt
        })
      }
      it("should return the latest object version lt as the ltCo") {
        ltCo should be({
          val (_, latestCo) = latestCandidate(ohsClassifiedAsInlineBarrier, barrierFlag = true, thresholds.r).get
          latestCo
        })
      }
      it("should return the latest object version lt as the current lt") {
        ltCur should be(lt)
      }
    }
  }


  //rh
  //empty rh
  describe("A Replica history") {
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
        latestTime(emptyRh) should be(emptyLT)
      }
    }
  }


  def checkOpType(ohsGen: Gen[OHS], opTypeToBe: OperationType): Assertion =
  /*without shrinking:
  forAll(ohsGen) { ohs => {
    val (opType, _, _) = classify(ohs, repairableThreshold = r, quorumThreshold = q)
    assert(opType == opType2)
  }*/
  //for solving bug https://www.47deg.com/blog/a-common-scalacheck-problem/, not using shrinking here:
    check {
      Prop.forAllNoShrink(ohsGen) { ohs => {
        val (opType, _, _) = classify(ohs, repairableThreshold = thresholds.r, quorumThreshold = thresholds.q)
        opType == opTypeToBe
      }
      }
    }

  def checkLatestObjCand(ohsGen: Gen[OHS]): Assertion =
    forAll(ohsGen) {
      ohs => {
        val (_, latestObjectCandidate, _) = classify(ohs, repairableThreshold = thresholds.r, quorumThreshold = thresholds.q)
        latestObjectCandidate should be(latestCandidate(ohs = ohs, barrierFlag = false, repairableThreshold = thresholds.r))
      }
    }

  def checkLatestBarCand(ohsGen: Gen[OHS]): Assertion = {
    forAll(ohsGen) {
      ohs => {
        val (_, _, latestBarrierCandidate) = classify(ohs, repairableThreshold = thresholds.r, quorumThreshold = thresholds.q)
        latestBarrierCandidate should be(latestCandidate(ohs = ohs, barrierFlag = true, repairableThreshold = thresholds.r))
      }
    }
  }


  //ohs
  describe("An OHS") {

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
    }

    def checkGreatestRepairableCandidate(ohsGen: Gen[OHS], barrierFlag: Boolean) = {
      forAll(ohsGen) { ohs =>
        val latest = latestCandidate(ohs, barrierFlag, thresholds.r)
        //elimino quelli che hanno ordine minore di r e dico e verifico che non esiste
        assert(!ohs
          .values
          .flatMap(e => e._1) //candidates here
          .filter(c => c._1.barrierFlag == barrierFlag)
          .filter(c => c > latest.getOrElse(c)) //if ohs has not a latestcandidate then test must fail
          .exists(order(_, ohs) >= thresholds.r))
      }
    }

    //latest_candidate
    describe("when queried for its latest object candidate") {
      it("should return the greatest, at least repairable, object candidate it contains") {
        checkGreatestRepairableCandidate(ohsGen, barrierFlag = false)
      }
      it("should return a object candidate and not a barrier candidate") {
        val barrierFlag = false
        forAll(ohsGen) { ohs =>
          for {
            (lt, _) <- latestCandidate(ohs, barrierFlag, thresholds.r)
          } yield lt.barrierFlag should be(barrierFlag)
        }
      }
    }
    describe("when queried for its latest barrier candidate") {
      it("should return the greatest, at least repairable, barrier candidate it contains") {
        checkGreatestRepairableCandidate(ohsGen, barrierFlag = true)
      }
      it("should return a barrier candidate and not a object candidate") {
        val barrierFlag = true
        forAll(ohsGen) { ohs =>
          for {
            (lt, _) <- latestCandidate(ohs, barrierFlag, thresholds.r)
          } yield lt.barrierFlag should be(barrierFlag)
        }
      }

    }


    describe("An empty OHS") {

      //---classify---
      describe("when classified") {
        it("should trigger method") {
          val (opType, _, _) = classify(emptyOhs(serversIds.toSet),
            repairableThreshold = thresholds.r,
            quorumThreshold = thresholds.q)
          opType should be(METHOD)
        }
        it("should return None as it latest barrier candidate") {
          val (_, _, latestBarrierCandidate) = classify(emptyOhs(serversIds.toSet),
            repairableThreshold = thresholds.r,
            quorumThreshold = thresholds.q)
          latestBarrierCandidate should be(Option.empty[Candidate])
        }
        it("should return the empty candidate as it latest object candidate") {
          val (_, latestObjectCandidate, _) = classify(emptyOhs(serversIds.toSet),
            repairableThreshold = thresholds.r,
            quorumThreshold = thresholds.q)
          latestObjectCandidate should be(Some(emptyCandidate))
        }
      }
      //latest candidate
      describe("when queried for its latest barrier candidate") {
        it("should return None") {
          latestCandidate(emptyOhs(serversIds.toSet), barrierFlag = true, thresholds.r) should be(Option.empty[Candidate])
        }
        it("should return the empty candidate as it latest objet candidate") {
          latestCandidate(emptyOhs(serversIds.toSet), barrierFlag = false, thresholds.r) should be(Some(emptyCandidate))
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
              represent(ohs) should be(represent(ohs))
            }
          }
        }
      }
    }

    val opGen = Gen.oneOf(IncrementAsObj, new GetObj)
    describe("An Operation representation") {
      describe("when compared to another representation of it") {
        it("should be equals to it") {
          forAll(opGen) { op =>
            represent(Some(op)) should be(represent(Some(op)))
          }
        }
      }
    }
  }
}
