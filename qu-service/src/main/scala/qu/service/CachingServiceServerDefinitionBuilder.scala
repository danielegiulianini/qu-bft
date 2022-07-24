package qu.service

import io.grpc.{MethodDescriptor, ServerCallHandler, ServerServiceDefinition}


/**
 * A [[io.grpc.ServerServiceDefinition]] capable of caching [[io.grpc.MethodDescriptor]]s as to provide better
 * performances.
 * @param serviceName the name of the service to add [[io.grpc.MethodDescriptor]] to.
 */
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