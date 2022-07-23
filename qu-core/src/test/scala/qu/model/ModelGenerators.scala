package qu.model

import org.scalacheck.{Arbitrary, Gen}
import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp, Flag, OHSRepresentation, OperationRepresentation, emptyLT}

import scala.math.Ordered.orderingToOrdered

trait ModelGenerators {

  def ltGenerator(timeGen: Gen[Int],
                  barrierGen: Gen[Boolean],
                  clientGen: Gen[Option[String]],
                  opGen: Gen[Option[String]],
                  ohsGen: Gen[Option[String]]): Gen[ConcreteLogicalTimestamp] =
    for {
      time <- timeGen
      barrierFlag <- barrierGen
      clientId <- clientGen
      operation <- opGen
      ohs <- ohsGen
    } yield ConcreteLogicalTimestamp(time, barrierFlag, clientId, operation, ohs)

  def customizableArbitraryLt(timePred: Int => Boolean = truePredicate(),
                              barrierPred: Boolean => Boolean = truePredicate(),
                              clientPred: Option[String] => Boolean = truePredicate(),
                              opReprPred: Option[String] => Boolean = truePredicate(),
                              ohsReprPred: Option[String] => Boolean = truePredicate()): Gen[ConcreteLogicalTimestamp] = {
    ltGenerator(
      Arbitrary.arbitrary[Int] suchThat (time => time >= 0 && timePred(time)),
      Arbitrary.arbitrary[Boolean] suchThat (barrierPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (clientPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (opReprPred(_)),
      Arbitrary.arbitrary[Option[String]] suchThat (ohsReprPred(_)))
  }

  def nonEmptyLt: Gen[ConcreteLogicalTimestamp] = arbitraryLt.suchThat(_ != emptyLT)

  def nonEmptyCandidate: Gen[(ConcreteLogicalTimestamp, ConcreteLogicalTimestamp)] = for {
    lt <- nonEmptyLt
    ltCo <- nonEmptyLt
  } yield (lt, ltCo)

  def truePredicate[T](): T => Flag = (_: T) => true

  //constraining values 1. to ease finding them by scalacheck engine and 2. to respect comparison properties in inner for
  val MaxLtTime = 2000
  val ltTimeGen: Gen[Int] = Gen.choose(0, MaxLtTime)

  def arbitraryOptionOfStringGen: Gen[Option[String]] = Gen.option(Gen.stringOfN(100, Gen.alphaChar))

  def arbitraryClientIdGen: Gen[Option[String]] = arbitraryOptionOfStringGen

  def arbitraryOpReprGen: Gen[Option[String]] = arbitraryOptionOfStringGen

  def arbitraryOhsReprGen: Gen[Option[String]] = arbitraryOptionOfStringGen //Gen.oneOf(Some("ohsRepr1"), Some("ohsRepr2"))

  def customArbitraryLtWithDefaults(timeGen: Gen[Int] = ltTimeGen, barrierGen: Gen[Boolean] = Arbitrary.arbitrary[Boolean], clientIdGen: Gen[Option[String]] = Gen.option(Gen.stringOfN(100, Gen.alphaChar)), opReprGen: Gen[Option[OperationRepresentation]] = Gen.option(Gen.stringOfN(100, Gen.alphaChar)), ohsReprGen: Gen[Option[OHSRepresentation]] = Gen.option(Gen.stringOfN(100, Gen.alphaChar))): Gen[ConcreteLogicalTimestamp] = {
    //constraining values 1. to ease finding them by scalacheck engine and 2. to respect comparison properties in inner for
    ltGenerator(
      ltTimeGen,
      barrierGen,
      clientIdGen,
      opReprGen,
      ohsReprGen
    )
  }

  def arbitraryLt: Gen[ConcreteLogicalTimestamp] =
  //constraining values 1. to ease finding them by scalacheck engine and 2. to respect comparison properties in inner for
    ltGenerator(
      ltTimeGen,
      Arbitrary.arbitrary[Boolean],
      arbitraryClientIdGen,
      arbitraryOpReprGen,
      arbitraryOhsReprGen,
    )

  val arbitraryLtInfo: Gen[(Int, Flag, Option[OperationRepresentation], Option[OperationRepresentation], Option[OperationRepresentation])] = for {
    time <- ltTimeGen
    barrierFlag <- Arbitrary.arbitrary[Boolean]
    clientId <- Arbitrary.arbitrary[Option[String]]
    operationRepresentation <- Arbitrary.arbitrary[Option[String]]
    ohsRepresentation <- Arbitrary.arbitrary[Option[String]]} yield (time, barrierFlag, clientId, operationRepresentation, ohsRepresentation)

  def logicalTimestampWithGreaterTimeThan(time: Int): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Gen.choose(time, Int.MaxValue),
    Arbitrary.arbitrary[Boolean],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierOfAndClientIdGreaterThan(time: Int, barrier: Boolean, clientId: Option[String]): Gen[ConcreteLogicalTimestamp]
  = ltGenerator(
    Gen.const(time),
    Gen.const(barrier),
    Arbitrary.arbitrary[Option[String]] suchThat (_ > clientId),
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierAndClientIdOfAndOpGreaterThan(time: Int, barrier: Boolean, clientId: Option[String], operationRepresentation: Option[OperationRepresentation]): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Gen.const(time),
    Gen.const(barrier),
    Gen.const(clientId),
    Arbitrary.arbitrary[Option[String]] suchThat (_ > operationRepresentation),
    Arbitrary.arbitrary[Option[String]])

  def ltWithSameTimeAndBarrierAndClientIdAndOpOfAndOhsGreaterThan(time: Int, barrier: Boolean, clientId: Option[String], operationRepresentation: Option[OperationRepresentation], ohsRepresentation: Option[OHSRepresentation]): Gen[ConcreteLogicalTimestamp] =
    ltGenerator(
      Gen.const(time),
      Gen.const(barrier),
      Gen.const(clientId),
      Gen.const(operationRepresentation),
      Arbitrary.arbitrary[Option[String]] suchThat (_ > ohsRepresentation))


  def sameLtForAllButTime(lt: ConcreteLogicalTimestamp): Gen[ConcreteLogicalTimestamp] =
    customArbitraryLtWithDefaults(
      timeGen = ltTimeGen.suchThat(_ != lt.time),
      barrierGen = Gen.const(lt.barrierFlag),
      clientIdGen = Gen.const(lt.clientId),
      opReprGen = Gen.const(lt.operation),
      ohsReprGen = Gen.const(lt.ohs)
    )

  def sameLtForAllButBarrierFlag(lt: ConcreteLogicalTimestamp): Gen[ConcreteLogicalTimestamp] =
    customArbitraryLtWithDefaults(
      timeGen = Gen.const(lt.time),
      barrierGen = Arbitrary.arbitrary[Boolean].suchThat(_ != lt.barrierFlag),
      clientIdGen = Gen.const(lt.clientId),
      opReprGen = Gen.const(lt.operation),
      ohsReprGen = Gen.const(lt.ohs)
    )

  def sameLtForAllButOhsRepr(lt: ConcreteLogicalTimestamp): Gen[ConcreteLogicalTimestamp] =
    customArbitraryLtWithDefaults(
      timeGen = Gen.const(lt.time),
      barrierGen = Gen.const(lt.barrierFlag),
      clientIdGen = arbitraryClientIdGen.suchThat(_ != lt.clientId),
      opReprGen = Gen.const(lt.operation),
      ohsReprGen = Gen.const(lt.ohs)
    )

  def sameLtForAllButOpRepr(lt: ConcreteLogicalTimestamp): Gen[ConcreteLogicalTimestamp] =
    customArbitraryLtWithDefaults(
      timeGen = Gen.const(lt.time),
      barrierGen = Gen.const(lt.barrierFlag),
      clientIdGen = Gen.const(lt.clientId),
      opReprGen = arbitraryOpReprGen.suchThat(_ != lt.operation),
      ohsReprGen = Gen.const(lt.ohs)
    )


  def ltWithSameTimeOfAndTrueBarrierFlag(time: Int): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Gen.const(time),
    Gen.const(true),
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]],
    Arbitrary.arbitrary[Option[String]])


  def sameLtForAllButClientId(lt: ConcreteLogicalTimestamp): Gen[ConcreteLogicalTimestamp] =
    customArbitraryLtWithDefaults(
      timeGen = Gen.const(lt.time),
      barrierGen = Gen.const(lt.barrierFlag),
      clientIdGen = Gen.const(lt.clientId),
      opReprGen = Gen.const(lt.operation),
      ohsReprGen = arbitraryOhsReprGen.suchThat(_ != lt.ohs)
    )


}
