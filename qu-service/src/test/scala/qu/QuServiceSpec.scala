package qu

import org.scalatest.funspec.AnyFunSpec


class QuServiceSpec extends AnyFunSpec {

  //type information survives network transit
  describe("A World") {
    describe("when initialized") {
      it("should have empty history") {
        //assert(initialWorld.worldHistory.isEmpty)
      }

      it("should be at 0 currentIteration") {
        //assert(initialWorld.currentIteration == 0)
      }
    }
  }

  //***BASIC FUNCTIONING***
  //barrier always accepted

  //server culls replica

  //stall ohs triggers fail

  //authenticator computed correctly

  //object sync

  //***OPTIMIZATIONS***

  //inline repair

  //repeated requests

  //***ADVANCED FUNCTIONING***

}
