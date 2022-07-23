package presentation

import io.grpc.MethodDescriptor

/**
 * A (GoF) factory for easing unary [[io.grpc.MethodDescriptor]] creation.
 * @tparam Transportable the higher-kinded type of the strategy responsible for messages (de)serialization.
 */
trait MethodDescriptorFactory[Transportable[_]] {
  self: MarshallerFactory[Transportable] =>

  //a deterministic function on parameter types used to identify method descriptors
  def genericTypesIdentifier[ReqT: Transportable, RespT: Transportable]: String

  def generateMethodDescriptor[ReqT: Transportable, RespT: Transportable](methodName: String, serviceName: String):
  MethodDescriptor[ReqT, RespT] = {
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
