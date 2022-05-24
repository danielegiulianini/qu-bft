package qu

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.MethodDescriptor

trait MethodDescriptorFactory[Transferable[_]] {
  self: MarshallerFactory[Transferable] =>

  //a deterministic function on parameter types
  def genericTypesIdentifier[ReqT: Transferable, RespT: Transferable]: String

  def generateMethodDescriptor5[ReqT: Transferable, RespT: Transferable](methodName: String, serviceName: String):
  MethodDescriptor[ReqT, RespT] = {
println("calling ... with name:" + MethodDescriptor.generateFullMethodName(serviceName,
  methodName + genericTypesIdentifier[ReqT, RespT]))
    MethodDescriptor.newBuilder(
      marshallerFor[ReqT],
      marshallerFor[RespT])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName,
        methodName + genericTypesIdentifier[ReqT, RespT]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build
  }

}

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
trait CachingMethodDescriptorFactory[Transferable[_]] extends MethodDescriptorFactory[Transferable]
  with MarshallerFactory[Transferable] {
  override def generateMethodDescriptor5[ReqT: Transferable, RespT: Transferable](methodName: String, serviceName: String):
  MethodDescriptor[ReqT, RespT] =
  //super refers to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor5[ReqT, RespT](methodName, serviceName))(genericTypesIdentifier[ReqT, RespT])
}


//family polymorphism:
trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory[JavaTypeable] with JacksonMarshallerFactory {
  override def genericTypesIdentifier[ReqT: JavaTypeable, RespT: JavaTypeable]: String =
    (typeIdentifier[ReqT]() + typeIdentifier[RespT]()).replace("/", "")

  private def typeIdentifier[T: JavaTypeable](): String =
    implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature
}