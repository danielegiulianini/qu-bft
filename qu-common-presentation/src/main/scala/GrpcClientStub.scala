import GrpcClientStub.{methodName, serviceName}
import io.grpc.{CallOptions, ManagedChannel}
import qu.protocol.{CostantiMomentanee, JacksonMethodDescriptorFactory, MarshallerFactory, MethodDescriptorFactory}
import scalapb.grpc.ClientCalls

import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._

abstract class GrpcClientStub[U](var chan: ManagedChannel) extends MethodDescriptorFactory with MarshallerFactory {
  //with grpc-java (listenableFuture) API
  def send[T](operation: Request[T, U],
              callOptions: CallOptions = CallOptions.DEFAULT) //default parameter value
             (implicit enc: Marshallable[T],
              dec: Marshallable[U],
              marshallable: Marshallable[Request[T, U]],
              marshallable3: Marshallable[Response[T,U]]):
  Future[Response[T,U]] = {
    val md = generateMethodDescriptor[T, U](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, operation)
  }
}

object GrpcClientStub {
  //decide where to inject (are grpc-specific constants)
  /*val methodName = "request"
  val serviceName = "io.grpc.KeyValueService"*/
  val methodName = CostantiMomentanee.METHOD_NAME
  val serviceName = CostantiMomentanee.SERVICE_NAME
}

class JacksonClientStub[A](channel: ManagedChannel)
  extends GrpcClientStub[A](channel) with JacksonMethodDescriptorFactory


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