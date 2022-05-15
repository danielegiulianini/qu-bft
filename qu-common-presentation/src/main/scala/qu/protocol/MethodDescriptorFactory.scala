package qu.protocol

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.MethodDescriptor

trait MethodDescriptorFactory[Transferable[_]] {
  self: MarshallerFactory[Transferable] =>

  //a deterministic function on parameter types...
  def genericTypesIdentifier[ReqT: Transferable, RespT: Transferable]: String //type Signaturable[T, U] <: {def typeIdentifier: String}

  def generateMethodDescriptor5[ReqT, RespT](methodName: String, serviceName: String)
                                            (implicit enc: Transferable[ReqT],
                                                enc3: Transferable[RespT]):
  MethodDescriptor[ReqT, RespT] =
    MethodDescriptor.newBuilder(
      marshallerFor[ReqT],
      marshallerFor[RespT])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + "ciaociao")) // getGenericSignature[InputT1, InputT2])) //todo
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build

}

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
/*trait CachingMethodDescriptorFactory[Marshallable[_]] extends MethodDescriptorFactory[Marshallable]
  with MarshallerFactory[Marshallable] {
  //i need an identifier of the pair of methods
  //override abstract is required here?
  override def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                             (implicit enc: Marshallable[Request[T, U]],
                                              enc3: Marshallable[Response[Option[T]]],
                                              enc2: Marshallable[T], dec: Marshallable[U]):
  MethodDescriptor[Request[T, U], Response[Option[T]]] =
  //super refers to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor(methodName, serviceName))(getGenericSignature[T, U])
}*/


//family polymorphism:
trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory[JavaTypeable] with JacksonMarshallerFactory {
  override def genericTypesIdentifier[ReqT: JavaTypeable, RespT: JavaTypeable]: String = {
    def typeIdentifier[T](): String = implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature
    (typeIdentifier[ReqT]() + typeIdentifier[RespT]()).replace("/", "")
  }

}