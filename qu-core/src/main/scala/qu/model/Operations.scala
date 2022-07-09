package qu.model

//inheritance and command pattern...
trait Operations {
  self: AbstractAbstractQuModel =>


  trait MyOperation[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): (ObjectT, ReturnValueT)
  }


  override type Operation[ReturnValueT, ObjectT] = MyOperation[ReturnValueT, ObjectT]

  trait AbstractOperation[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT] {
    //template method pattern
    override def compute(obj: ObjectT): (ObjectT, ReturnValueT) = (updateObject(obj), whatToReturn(obj))

    def updateObject(obj: ObjectT): ObjectT

    def whatToReturn(obj: ObjectT): ReturnValueT
  }

  trait Update[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT]

  trait Query[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT] {
    final override def updateObject(obj: ObjectT): ObjectT = obj
  }

  //reusable utilities that returns object at server side
  trait UpdateReturningObject[ObjectT] extends Update[ObjectT, ObjectT]

  trait QueryReturningObject[ObjectT] extends Query[ObjectT, ObjectT]
    with OperationReturningUnUpdatedObject[ObjectT]

  trait UpdateReturningUnUpdatedObject[ObjectT] extends OperationReturningUnUpdatedObject[ObjectT]

  trait UpdateReturningUnit[ObjectT] extends Update[Unit, ObjectT] {
    final override def whatToReturn(obj: ObjectT): Unit = {}
  }

  trait OperationReturningUnUpdatedObject[ObjectT] extends AbstractOperation[ObjectT, ObjectT] {
    override def whatToReturn(obj: ObjectT): ObjectT = obj
  }

}
