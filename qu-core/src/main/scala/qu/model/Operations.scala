package qu.model

/**
 * A trait containing abstractions to realize a Replicated State Machine upon which to build the Q/U service.
 */
trait Operations {

  self: LessAbstractQuModel =>

  /**
   * Abstract modelling of an operation that can be submitted to Q/U replicas, for implementing
   * a Replicated State Machine, leveraging (GoF) command pattern.
   * @tparam ReturnValueT the type of the value returned by Operation invocation on object.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait MyOperation[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): (ObjectT, ReturnValueT)
  }


  override type Operation[ReturnValueT, ObjectT] = MyOperation[ReturnValueT, ObjectT]

  /**
   * Refinement of [[qu.model.Operations.MyOperation]] defining its compute method as a (GoF) template method.
   * @tparam ReturnValueT the type of the value returned by Operation invocation on object.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait AbstractOperation[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT] {
    //GoF template method
    override def compute(obj: ObjectT): (ObjectT, ReturnValueT) = (updateObject(obj), whatToReturn(obj))

    def updateObject(obj: ObjectT): ObjectT

    def whatToReturn(obj: ObjectT): ReturnValueT
  }

  /**
   * Models a [[qu.model.QuModel.Operation[ReturnValueT, ObjectT]] that updates the state of the object on which it
   * is invoked.
   * @tparam ReturnValueT the type of the value returned by Operation invocation on object.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait Update[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT]



  /**
   * Models a [[qu.model.QuModel.Operation[ReturnValueT, ObjectT]] that does not update the state of the object
   * on which it is invoked.
   * @tparam ReturnValueT the type of the value returned by Operation invocation on object.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait Query[ReturnValueT, ObjectT] extends AbstractOperation[ReturnValueT, ObjectT] {
    final override def updateObject(obj: ObjectT): ObjectT = obj
  }

  //some reusable utilities that returns object at server side

  /**
   * Models a [[qu.model.Operations.Update]] whose invocation returns a value of the same type of the object on which
   * it is performed.
   * @tparam ObjectT the type of the object on which operation is invoked as well as that of the value returned
   *                 by the invocation.
   */
  trait UpdateReturningObject[ObjectT] extends Update[ObjectT, ObjectT]

  /**
   * Models a [[qu.model.Operations.Query]] whose invocation returns a value of the same type of the object on which
   * it is performed.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait QueryReturningObject[ObjectT] extends Query[ObjectT, ObjectT]
    with OperationReturningUnUpdatedObject[ObjectT]

  /**
   * Models a [[qu.model.Operations.Update]] whose invocation returns a value of the same type of the object on which
   * it is performed.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait UpdateReturningUnUpdatedObject[ObjectT] extends OperationReturningUnUpdatedObject[ObjectT]

  /**
   * Models a [[qu.model.Operations.Update]] whose invocation returns unit.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait UpdateReturningUnit[ObjectT] extends Update[Unit, ObjectT] {
    final override def whatToReturn(obj: ObjectT): Unit = {}
  }

  /**
   * Models a [[qu.model.Operations.MyOperation]] returning the object state resulting from the operation invocation on which
   * this trait is injected without any further manipulation.
   * @tparam ObjectT the type of the object on which the operations is invoked.
   */
  trait OperationReturningUnUpdatedObject[ObjectT] extends AbstractOperation[ObjectT, ObjectT] {
    override def whatToReturn(obj: ObjectT): ObjectT = obj
  }

}
