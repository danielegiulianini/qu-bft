package qu.service

import io.grpc.{MethodDescriptor, ServerCallHandler, ServerServiceDefinition}

class CachingServiceServerDefinitionBuilder(private var serviceName: String) {
  private val builder = ServerServiceDefinition.builder(serviceName)
  private var mds = Set[String]()

  def addMethod[ReqT, RespT](`def`: MethodDescriptor[ReqT, RespT], handler: ServerCallHandler[ReqT, RespT]): CachingServiceServerDefinitionBuilder = {
    if (!mds.contains(`def`.getFullMethodName)) {
      mds = mds + `def`.getFullMethodName
      builder.addMethod(`def`, handler)
    }
    this
  }

  def build(): ServerServiceDefinition = builder.build
}

object CachingServiceServerDefinitionBuilder {
  def apply(serviceName: String) =
    new CachingServiceServerDefinitionBuilder(serviceName)
}