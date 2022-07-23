package qu.model

import org.scalacheck.Gen
import qu.model.ConcreteQuModel.{OHS, emptyOhs}

trait OhsGenerators {

  self: OHSUtilities with FourServersScenario =>

  //can be replaced with a more sophisticated generation approach (but using generators so it's already set for it)
  val ohsWithMethodGen: Gen[Map[ConcreteQuModel.ServerId, (List[(ConcreteQuModel.ConcreteLogicalTimestamp, ConcreteQuModel.ConcreteLogicalTimestamp)], Map[ConcreteQuModel.ServerId, ConcreteQuModel.hMac])]] =
    Gen.oneOf(ohsWithMethodFor(serversKeys), emptyOhs(serversIds.toSet))

  val ohsWithInlineMethodGen: Gen[OHS] = Gen.const(ohsWithInlineMethodFor(serversKeys, thresholds.r))

  val ohsWithInlineBarrierGen: Gen[OHS] = Gen.const(ohsWithInlineBarrierFor(serversKeys, thresholds.r))

  val ohsWithBarrierGen: Gen[OHS] = Gen.const(ohsWithBarrierFor(serversKeys))

  val ohsWithCopyGen: Gen[OHS] = Gen.const(ohsWithCopyFor(serversKeys))

  val ohsGen: Gen[OHS] = Gen.oneOf(ohsWithMethodGen,
    ohsWithInlineMethodGen,
    ohsWithInlineBarrierGen,
    ohsWithBarrierGen,
    ohsWithCopyGen)
}
