package qu.model

import org.scalatest.funspec.AnyFunSpec
import qu.model.ConcreteQuModel._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Prop.forAll
import org.scalacheck.Test.Parameters.default.minSuccessfulTests
import org.scalacheck.util.Pretty.Params
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{convertToAnyShouldWrapper, equal}
import org.scalacheck.{Gen, Prop}
import org.scalatest.prop.TableDrivenPropertyChecks.whenever
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}
import org.scalatestplus.scalacheck.Checkers.check
import qu.model.Commands.{GetObj, IncrementAsObj}
import qu.model.SharedContainer.keysForServer

import scala.language.postfixOps
import scala.math.Ordered.orderingToOrdered
import scala.reflect.internal.Flags.METHOD

class QuModelSpec extends AnyFunSpec with ScalaCheckPropertyChecks /*with Checkers*/ with Matchers with OHSFixture2 {

  //todo: could improve passing only timestamp by (instead of all the params)
  def ltGenerator(timeGen: Gen[Int], barrierGen: Gen[Boolean], clientGen: Gen[Option[String]], opGen: Gen[Option[String]], ohsGen: Gen[Option[String]]) =
    for {
      time <- timeGen
      barrierFlag <- barrierGen
      clientId <- clientGen
      operation <- opGen
      ohs <- ohsGen
    } yield ConcreteLogicalTimestamp(time, barrierFlag, clientId, operation, ohs)

  def customizableArbitraryLt(timePred: Int => Boolean = truePred,
                              barrierPred: Boolean => Boolean = truePred,
                              clientPred: Option[String] => Boolean = truePred,
                              opReprPred: Option[String] => Boolean = truePred,
                              ohsReprPred: Option[String] => Boolean = truePred) = {
    ltGenerator(
      Arbitrary.arbitrary[Int] suchThat (time => time >= 0 && timePred(time)),
      Arbitrary.arbitrary[Boolean] suchThat (barrierPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (clientPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (opReprPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (ohsReprPred(_)))
  }

  def nonEmptyLt = arbitraryLt.suchThat(_ != emptyLT) //customizableArbitraryLt(timePred = _ != initi)

  def nonEmptyCandidate: Gen[(ConcreteLogicalTimestamp, ConcreteLogicalTimestamp)] = for {
    lt <- nonEmptyLt
    ltCo <- nonEmptyLt
  } yield (lt, ltCo)

  def truePred[T](): T => Flag = (_: T) => true

  val arbitraryLt: Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Arbitrary.arbitrary[Int] suchThat (_ >= 0),
    Arbitrary.arbitrary[Boolean] suchThat truePred(),
    Arbitrary.arbitrary[Option[String]] suchThat truePred(),
    Arbitrary.arbitrary[Option[String]] suchThat truePred(),
    Arbitrary.arbitrary[Option[String]] suchThat truePred())
  /*ltGenerator(
    Arbitrary.arbitrary[Int] suchThat (_ >= 0),
    Arbitrary.arbitrary[Boolean],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])*/

  val arbitraryLtInfo: Gen[(Int, Flag, Option[OperationRepresentation], Option[OperationRepresentation], Option[OperationRepresentation])] = for {
    a <- Arbitrary.arbitrary[Int] suchThat (_ >= 0)
    b <- Arbitrary.arbitrary[Boolean]
    c <- Arbitrary.arbitrary[Option[String]]
    d <- Arbitrary.arbitrary[Option[String]]
    e <- Arbitrary.arbitrary[Option[String]]} yield (a, b, c, d, e)

  def logicalTimestampWithGreaterTimeThan(time: Int): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Arbitrary.arbitrary[Int] suchThat (tme => tme >= 0 && tme > time),
    Arbitrary.arbitrary[Boolean],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeOfAndBarrierGreaterThan(time: Int, barrierFlag: Boolean): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Gen.const(time),
    Arbitrary.arbitrary[Boolean] suchThat (_ > barrierFlag),
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierOfAndClientIdGreaterThan(time: Int, barrier: Boolean, clientId: Option[String])
  = ltGenerator(
    Gen.const(time),
    Gen.const(barrier),
    Arbitrary.arbitrary[Option[String]] suchThat (_ > clientId),
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierAndClientIdOfAndOpGreaterThan(time: Int, barrier: Boolean, clientId: Option[String], operationRepresentation: Option[OperationRepresentation]) = ltGenerator(
    Gen.const(time),
    Gen.const(barrier),
    Gen.const(clientId),
    Arbitrary.arbitrary[Option[String]] suchThat (_ > operationRepresentation),
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierAndClientIdAndOpOfAndOhsGreaterThan(time: Int, barrier: Boolean, clientId: Option[String], operationRepresentation: Option[OperationRepresentation], ohsRepresentation: Option[OHSRepresentation]) =
    ltGenerator(
      Gen.const(time),
      Gen.const(barrier),
      Gen.const(clientId),
      Gen.const(operationRepresentation),
      Arbitrary.arbitrary[Option[String]] suchThat (_ > ohsRepresentation))


  //todo can be replaced with a nested generator
  val aLt = ConcreteLogicalTimestamp(10, false, Some("client1"), Some("query"), Some("ohs")) //time: Int, barrierFlag: Boolean, clientId: Option[ClientId],operation: Option[OperationRepresentation],ohs: Option[OHSRepresentation])
  describe("A ConcreteLogicalTimestamp") {
    //test ordering
    describe("when initialized with a logical time") {
      it("should be classified as previous to a ConcreteLogicalTimestamp with a greater logical time") {
        forAll(logicalTimestampWithGreaterTimeThan(aLt.time)) { lt =>
          println("tested lt: " + lt)
          lt < aLt
        } //assert(aLt>aLt2)
      }

      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical time and a greater barrier flag") {
        forAll(ltWithSameTimeOfAndBarrierGreaterThan(aLt.time, aLt.barrierFlag)) { lt =>
          println("tested lt: " + lt)
          lt < aLt
        }
      }

      //todo verify if actually it generates them
      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, same barrier flag but lexicographically greater clientId") {
        println("hello")
        forAll(ltWithSameTimeAndBarrierOfAndClientIdGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId)) { lt =>
          println("tested lt: " + lt)
          lt < aLt //_ < aLt
        }
      }


      it("should be classified as previous to a ConcreteLogicalTimestamp with same logical, barrier flag, clientId, but lexicographically greater OperationRepresentation") {
        forAll(ltWithSameTimeAndBarrierAndClientIdOfAndOpGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId, aLt.operation)) {
          _ < aLt
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
            _ < aLt
          }
        }
        //forAll(ltWithSameTimeAndBarrierAndClientIdAndOpOfAndOhsGreaterThan(aLt.time, aLt.barrierFlag, aLt.clientId, aLt.operation, aLt.ohs)){ _ < aLt }
        //}

      }

      //test equals (ordering doesn't affect equals so must check equality too)
      it("should be equal with a ConcreteLogicalTimestamp with same time, same barrierFlag, same clientId") {
        forAll(arbitraryLtInfo) { case (a, b, c, d, e) =>
          ConcreteLogicalTimestamp(a, b, c, d, e) == ConcreteLogicalTimestamp(a, b, c, d, e)
        }
      }

      it("should not be equal with a ConcreteLogicalTimestamp with same time, different barrierFlag") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
  }

  //candidate
  describe("A candidate") {

    describe("when it is set up from a ohs with an established barrier as latest ") {

      it("should ") {
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

  //todo can be shared with quClientSpec in ascenario case class
  val exampleServersIds = (1 to 4 toList).map("s" + _)
  val exampleServersKeys: Map[ServerId, Map[ConcreteQuModel.ServerId, ServerId]] =
    exampleServersIds.map(id => id -> keysForServer(id, exampleServersIds.toSet)).toMap

  val r = 2
  val q = 4
  //todo can be replaced with a more sophisticated generator approach
  val ohsGen = Gen.oneOf(ohsWithMethodGen,
    ohsWithInlineMethodGen,
    ohsWithInlineBarrierGen,
    ohsWithBarrierGen,
    ohsWithCopyGen)

  val ohsWithMethodGen = Gen.oneOf(
    ohsWithMethodFor(exampleServersKeys))

  val ohsWithInlineMethodGen = Gen.oneOf(
    ohsWithInlineMethodFor(exampleServersKeys, 2))

  val ohsWithInlineBarrierGen = Gen.oneOf(
    ohsWithInlineBarrierFor(exampleServersKeys, 2))

  val ohsWithBarrierGen = Gen.oneOf(
    ohsWithBarrierFor(exampleServersKeys))

  val ohsWithCopyGen = Gen.oneOf(
    ohsWithCopyFor(exampleServersKeys))
  //ohs
  //todo: can also be expressed on functions rather on obj (a <function invocation> (ex.classification) when ... should ...) or a <method> invocation when ... should ... or <method> , when invoked on ..., it should...
  describe("An OHS") {
    //equals
    //aggiornamento in base al server
    //---classify--- (testing all its branches)
    describe("when a established barrier is latest") {
      it("classification should trigger a copy") {
        forAll(ohsWithMethodGen) { ohs => {
          val (opType, _, _) = classify(ohs, repairableThreshold=r, quorumThreshold=q)
        opType == METHOD
        }

        }
      }

      it("should return its latest object candidate as latest object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    describe("when a established object is latest") {
      it("should trigger a method") {
        //assert(initialWorld.worldHistory.isEmpty)
      }

      it("should return its latest object candidate as latest object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    describe("when a repairable barrier is latest") {
      it("should trigger a method") {
        //assert(initialWorld.worldHistory.isEmpty)
      }

      it("should return its latest object candidate as latest object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    describe("when a repairable object is latest") {
      it("should trigger a method") {
        //assert(initialWorld.worldHistory.isEmpty)
      }

      it("should return its latest object candidate as latest object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }

      it("should return its latest barrier candidate as latest barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    //latest_candidate
    describe("when queried for its latest object candidate") {
      it("should return the greatest object candidate it contains") {
        //assert(initialWorld.currentIteration == 0)
      }
      it("should return a object candidate and not a barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    describe("when queried for its latest barrier candidate") {
      it("should return the greatest barrier candidate it contains") {
        //assert(initialWorld.currentIteration == 0)
      }
      it("should return a barrier candidate and not a object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    //latest time
  }


  describe("An empty OHS") {
    //since using object model.. here I must refer to the same structure as above
    //ordering and  comparing
    //se inserisco 2 volte in diverso ordine sono comunque uguali le ohs ...

    //---classify---
    describe("when classified") {
      it("should trigger method") {
        //assert(initialWorld.currentIteration == 0)
      }
      it("should not return a barrier candidate as it latest barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
      it("should return a the empty candidate as it latest object candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    //latest candidate
    describe("when queried for its latest barrier candidate") {
      it("should return an empty barrier candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
      it("should return a the empty candidate as it latest objet candidate") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
    //latest_time
    it("should return the empty Logical timestamp as it latest time") {
      assert(latestTime(emptyRh) == emptyLT)
    }
  }


  describe("An OHS representation") {
    describe("when compared to another representation of it") {
      it("should be equals to it") {
        forAll(ohsGen) { ohs =>
          assert(represent(ohs) == represent(ohs))
        }
      }
    }
  }

  val opGen = Gen.oneOf(IncrementAsObj, new GetObj)
  describe("An Operation representation") {
    describe("when compared to another representation of it") {
      it("should be equals to it") {
        forAll(opGen) { op =>
          assert(represent(Some(op)) == represent(Some(op)))
        }
      }
    }
  }


  //must test setup here
}