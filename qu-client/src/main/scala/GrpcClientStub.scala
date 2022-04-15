import com.fasterxml.jackson.module.scala.JavaTypeable
import com.google.common.util.concurrent.ListenableFuture
import io.grpc.{CallOptions, ManagedChannel, MethodDescriptor}
import qu.protocol.Messages.{Request, Response}
import qu.protocol.{JacksonMethodDescriptorFactory, MarshallerFactory, Messages, MethodDescriptorFactory}
import scalapb.grpc.ClientCalls

import scala.concurrent.Future


abstract class GrpcClientStub[T](var chan: ManagedChannel) {
  self: MethodDescriptorFactory with MarshallerFactory =>

  //decide where to inject
  val methodName = "todo"
  val serviceName = "todo"

  //with grpc-java (listenableFuture) API
  def send[U](operation: Messages.Request[T, U],
              callOptions: CallOptions = CallOptions.DEFAULT) //default parameter value
             (implicit enc: Marshallable[T],
              dec: Marshallable[U],
              marshallable: Marshallable[Request[T, U]],
              marshallable3: Marshallable[Response[T]]):
  Future[Messages.Response[T]] = {
    val md = generateMethodDescriptor[T, U](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, operation)
  }
}

abstract class JacksonClientStub(channel: ManagedChannel)
  extends GrpcClientStub(channel) with JacksonMethodDescriptorFactory


//example of use:
//class JacksonCLientStub extends GrpcClientStub(null) with JacksonMethodDescriptorFactory

//with cake pattern:
//class JacksonCLientStub extends GrpcClientStub(null) with JacksonMethodDescriptorFactory {
//val a = new A (declared in GrpcClientStub); val b= new B (declared in GrpcClientStub))
//}
//only diff with normal mixin with self-type is: every mixin doesn't call methods directly
//on the self type but on its val


/*
*  //with grpc-java (listenableFuture) API
  def send[U](op: Messages.Request[T, U],
              callOptions: CallOptions = CallOptions.DEFAULT) //default parameter value
             (implicit enc: Marshallable[T],
              dec: Marshallable[U],
              marshallable: Marshallable[Request[T, U]],
              marshallable3: Marshallable[Response[T]]):
  ListenableFuture[Messages.Response[T]] = {
    val md2 = generateMethodDescriptor[T, U](methodName, serviceName)
    ClientCalls.futureUnaryCall(chan.newCall(md2, callOptions), op)
  }*/