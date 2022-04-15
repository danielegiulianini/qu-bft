import com.fasterxml.jackson.module.scala.JavaTypeable
import com.google.common.util.concurrent.ListenableFuture
import io.grpc.stub.ClientCalls
import io.grpc.{CallOptions, ManagedChannel, MethodDescriptor}
import qu.protocol.Messages.{Request, Response}
import qu.protocol.{JacksonMethodDescriptorFactory, MarshallerFactory, Messages, MethodDescriptorFactory}


abstract class GrpcClientStub[T](var chan: ManagedChannel) {
  self: MethodDescriptorFactory with MarshallerFactory =>

  //decide where to inject
  val methodName = "todo"
  val serviceName = "todo"

  def send[U](op: Messages.Request[T, U])
             (implicit enc: Marshallable[T], dec: Marshallable[U],
              marshallable: Marshallable[Request[T, U]],
              marshallable3: Marshallable[Response[T]]):
  ListenableFuture[Messages.Response[T]] = {
    val md2 = generateMethodDescriptor[T, U](methodName, serviceName)
    ClientCalls.futureUnaryCall(chan.newCall(md2, CallOptions.DEFAULT), op)
  }
}

class JacksonClientStub(channel: ManagedChannel) extends GrpcClientStub(channel) with JacksonMethodDescriptorFactory


//example of use:
//class JacksonCLientStub extends GrpcClientStub(null) with JacksonMethodDescriptorFactory

//with cake pattern:
//class JacksonCLientStub extends GrpcClientStub(null) with JacksonMethodDescriptorFactory {
//val a = new A (declared in GrpcClientStub); val b= new B (declared in GrpcClientStub))
//}
//only diff with normal mixin with self-type is: every mixin doesn't call methods directly
//on the self type but on its val