package presentation

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import qu.JacksonMarshallerFactory


//testing critical classes (de)serialization
class PresentationSpec extends AnyFunSpec with Matchers {
  describe("a Some(Unit)") {
    describe("when serialized and deserialized") {
      it("should return a Some(Unit)") {
        val marshaller = new JacksonMarshallerFactory {}.marshallerFor[Some[Unit]]
        marshaller.parse(marshaller.stream(Some())) should be(Some(()))
      }
    }
  }
}
