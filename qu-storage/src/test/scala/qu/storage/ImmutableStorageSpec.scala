package qu.storage

import org.scalatest.funspec.AnyFunSpec
import qu.model.QuorumSystemThresholdQuModel.ConcreteLogicalTimestamp

class ImmutableStorageSpec extends AnyFunSpec with StorageFixture {

  //store, retrieveObject, retrieve
  /* def store[T: TypeTag](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Storage[U]

   def retrieve[T: TypeTag](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])]

   def retrieveObject(logicalTimestamp: LogicalTimestamp): Option[U]*/

  val aLt: ConcreteLogicalTimestamp = ConcreteLogicalTimestamp(2, barrierFlag = false, Some("id1"), Option.empty, Option.empty)
  val anotherLt: ConcreteLogicalTimestamp = ConcreteLogicalTimestamp(3, barrierFlag = true, Some("id2"), Option.empty, Option.empty)


  describe("An immutable storage") {
    describe("when empty") {
      it("should not return any object-answer pair") {
        storage.retrieve[Int](aLt) should be(Option.empty[(Int, Option[Int])])
      }
      it("should not return any object") {
        storage.retrieveObject(aLt) should be(Option.empty[Int])

      }
    }

    describe("when it have previously stored an object-pair associated to a given timestamp") {
      it("should return the same pair when asked for the same timestamp") {
        storage.store[String](aLt, (2023, Some("2023"))).retrieve[String](aLt) should be (Some((2023, Some("2023"))))
      }
      it("should return the same object when asked for the same timestamp") {
        storage.store[String](aLt, (2023, Some("2023"))).retrieveObject(aLt) should be (Some(2023))
      }
    }

    describe("when it do not have stored an object against a given timestamp") {
      it("should not return it when asked for the same timestamp") {
        storage.store[String](aLt, (2023, Some("2023"))).retrieve[String](anotherLt) should be (Option.empty[(Int, Option[String])])
      }
      it("should not return the same object when asked for the same timestamp") {
        storage.store[String](aLt, (2023, Some("2023"))).retrieveObject(anotherLt) should be (Option.empty[Int])
      }
    }

    describe("when storing an object associated to a given timestamp") {
      it("should return the new, updated store") {
        storage.store[String](aLt, (2023, Some("2023"))) should not be storage
      }
      it("should not edit the original store") {
        val storageBeforeStoring = storage
        storage.store[String](aLt, (2023, Some("2023")))
        storage should be (storageBeforeStoring)
      }
    }
  }
}

//scalacheck: for every object that i insert then it could be retrieved
