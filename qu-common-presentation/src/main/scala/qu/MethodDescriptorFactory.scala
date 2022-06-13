package qu


import io.grpc.MethodDescriptor

trait MethodDescriptorFactory[Transferable[_]] {
  self: MarshallerFactory[Transferable] =>

  //a deterministic function on parameter types
  def genericTypesIdentifier[ReqT: Transferable, RespT: Transferable]: String

  def generateMethodDescriptor5[ReqT: Transferable, RespT: Transferable](methodName: String, serviceName: String):
  MethodDescriptor[ReqT, RespT] = {
    //println("generating for (request):  " + implicitly[Transferable[ReqT]] + "and response: " +implicitly[Transferable[ReqT]] )
//println("calling ... with name:" + MethodDescriptor.generateFullMethodName(serviceName,
  //methodName + genericTypesIdentifier[ReqT, RespT]))
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




