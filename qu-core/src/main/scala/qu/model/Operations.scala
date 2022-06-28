package qu.model

//inheritance and command pattern...
trait Operations {
  self: AbstractAbstractQuModel =>

  //good, but non working:
  // override type Operation[ReturnValueT, ObjectT] = ObjectT => (ObjectT, ReturnValueT)
  //trait MyOperation[ReturnValueT, ObjectT] extends (ObjectT => (ObjectT, ReturnValueT)) {
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

  //todo should verify that the whatToReturn has not side-effects
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


  //todo here on some perplexities...
  trait OperationReturningUnUpdatedObject[ObjectT] extends AbstractOperation[ObjectT, ObjectT] {
    //ciò varrebbe solo se facessi lavorare l'update su una copia difensiva
    override def whatToReturn(obj: ObjectT): ObjectT = obj
  }

}
