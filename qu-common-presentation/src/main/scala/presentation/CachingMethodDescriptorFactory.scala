package presentation

import io.grpc.MethodDescriptor

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
trait CachingMethodDescriptorFactory[Transferable[_]] extends MethodDescriptorFactory[Transferable]
  with MarshallerFactory[Transferable] {
  override def generateMethodDescriptor[ReqT: Transferable, RespT: Transferable](methodName: String, serviceName: String):
  MethodDescriptor[ReqT, RespT] =
  //super refers to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor[ReqT, RespT](methodName, serviceName))(genericTypesIdentifier[ReqT, RespT])
}
