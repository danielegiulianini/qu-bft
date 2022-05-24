package qu.model

trait Operations {
  self: AbstractAbstractQuModel =>

  override type Operation[ReturnValueT, ObjectT] = ObjectT => (ObjectT, ReturnValueT)

  trait AbstractOperation[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT] {
    //template method pattern
    override def apply(obj: ObjectT): (ObjectT, ReturnValueT) = (updateObject(obj), whatToReturn(obj))

    def updateObject(obj: ObjectT): ObjectT

    def whatToReturn(obj: ObjectT): ReturnValueT
  }

  trait Update[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT]

  //todo should verify that the whatToReturn has not side-effects
  trait Query[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT] {
    final override def updateObject(obj: ObjectT): ObjectT = obj
  }

  //reusable utilities that returns object at server side
  trait UpdateReturningObject[ObjectT] extends Update[ObjectT, ObjectT]

  trait QueryReturningObject[ObjectT] extends Query[ObjectT, ObjectT]
    with OperationReturningObjectWithoutUpdate[ObjectT]

  trait UpdateReturningUpdatedObject[ObjectT] extends OperationReturningObjectWithoutUpdate[ObjectT] {
    override def whatToReturn(obj: ObjectT): ObjectT = obj
  }

  trait UpdateReturningUnit[ObjectT] extends Update[Unit, ObjectT] {
    final override def whatToReturn(obj: ObjectT): Unit = {}
  }

  class GetObj[ObjectT]() extends QueryReturningObject[ObjectT]

  //todo here on some perplexities...
  trait OperationReturningObjectWithoutUpdate[ObjectT] extends AbstractOperation[ObjectT, ObjectT] {
    //ciò varrebbe solo se facessi lavorare l'update su una copia difensiva
    override def whatToReturn(obj: ObjectT): ObjectT = obj
  }

}
