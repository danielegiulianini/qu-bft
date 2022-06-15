import org.scalatest.funspec.AnyFunSpec

class SystemTesting extends AnyFunSpec {
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
}

