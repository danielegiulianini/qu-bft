package qu.model

import org.scalacheck.{Arbitrary, Gen}
import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp, Flag, OHSRepresentation, OperationRepresentation, emptyLT}

import scala.math.Ordered.orderingToOrdered

object ModelGenerators {

  //todo: could improve passing only timestamp by (instead of all the params)
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

  def nonEmptyLt: Gen[ConcreteLogicalTimestamp] = arbitraryLt.suchThat(_ != emptyLT) //customizableArbitraryLt(timePred = _ != initi)

  def nonEmptyCandidate: Gen[(ConcreteLogicalTimestamp, ConcreteLogicalTimestamp)] = for {
    lt <- nonEmptyLt
    ltCo <- nonEmptyLt
  } yield (lt, ltCo)

  def truePredicate[T](): T => Flag = (_: T) => true

  val MaxLtTime = 2000
  val ltTimeGen: Gen[Int] = Gen.choose(0, MaxLtTime)
  /*val opReprGen: Gen[Some[OperationRepresentation]] = Gen.oneOf(Some(represent[Unit, Int](Some(Increment()))), Some(represent[Int, Int](Some(GetObj()))))
  val ohsReprGen: Gen[Some[OperationRepresentation]] = Gen.oneOf(Some("ohsRepr1"), Some("ohsRepr2"))
  val clientIdGen = Gen.const(Some("client1"))*/

  def arbitraryLt: Gen[ConcreteLogicalTimestamp] = {
    //constraining values 1. to ease finding them by scalacheck engine and 2. to respect comparison properties in inner for
    ltGenerator(
      ltTimeGen,
      Arbitrary.arbitrary[Boolean],
      Gen.option(Gen.stringOfN(100, Gen.alphaChar)),//clientIdGen,
      Gen.option(Gen.stringOfN(100, Gen.alphaChar)),//opReprGen suchThat truePredicate(), //Arbitrary.arbitrary[Option[String]] suchThat truePred(), //OR gen.option(
      Gen.option(Gen.stringOfN(100, Gen.alphaChar))//ohsReprGen suchThat truePredicate() //Arbitrary.arbitrary[Option[String]] suchThat truePred())
    )
  }

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

  def ltWithSameTimeOfAndTrueBarrierFlag(time: Int): Gen[ConcreteLogicalTimestamp] = ltGenerator(
    Gen.const(time),
    Gen.const(true),
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

}
