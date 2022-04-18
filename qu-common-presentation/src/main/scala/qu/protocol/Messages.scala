package qu.protocol

//operation modeled via Gof's command pattern
object Messages {

  trait Operation[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): ReturnValueT
  }

  trait Query[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT]

  trait Update[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT]

  final case class Request[ReturnValueT, ObjectT](operation: Operation[ReturnValueT, ObjectT])

  final case class Response[ReturnValueT, ObjectT](responseCode:Int, answer: ReturnValueT, order: Int, authenticatedRh: ConcreteQuModel.AuthenticatedReplicaHistory[ObjectT])

}
