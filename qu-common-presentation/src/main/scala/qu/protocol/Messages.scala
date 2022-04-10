package qu.protocol

import com.fasterxml.jackson.annotation.JsonTypeInfo


object Messages {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
  trait Operation[ReturnValueT, ObjectT] {
    def compute(obj: ObjectT): ReturnValueT
  }

  trait Query[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT]

  trait Update[ReturnValueT, ObjectT] extends Operation[ReturnValueT, ObjectT]

  final case class Request[ReturnValueT, ObjectT](operation: Messages.Operation[ReturnValueT, ObjectT])

  final case class Response[ReturnValueT](responseCode:Int, answer: ReturnValueT, order: Int)
}
