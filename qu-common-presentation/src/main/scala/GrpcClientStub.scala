import GrpcClientStub.{methodName, serviceName}
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{CallOptions, ManagedChannel}
import qu.protocol.{JacksonMethodDescriptorFactory, MarshallerFactory, MethodDescriptorFactory, TemporaryConstants}
import scalapb.grpc.ClientCalls

import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._

//a stub reusable between client and server sides
abstract class GrpcClientStub[Marshallable[_]](var chan: ManagedChannel)
  extends MethodDescriptorFactory[Marshallable] with MarshallerFactory[Marshallable] {
  def send2[InputT, OutputT](toBeSent: InputT, callOptions: CallOptions = CallOptions.DEFAULT) //default parameter value
                            (implicit
                             enc: Marshallable[InputT],
                             dec: Marshallable[OutputT]):
  Future[OutputT] = {
    //must add timeout
    val md = generateMethodDescriptor5[InputT, OutputT](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }
}

object GrpcClientStub {
  //decide where to inject (are grpc-specific constants)
  /*val methodName = "request"
  val serviceName = "io.grpc.KeyValueService"*/
  val methodName = TemporaryConstants.METHOD_NAME
  val serviceName = TemporaryConstants.SERVICE_NAME

  class JacksonClientStub(channel: ManagedChannel)
    extends GrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory

  //esempio di metodo di conversione degli impliciti (send2 li richiede, prova no)
  def prova[T, U]() = {
    val a = new JacksonClientStub(null)
    a.send2[Request[T, U], Response[Option[T]]](new Request[T, U](null, null))
  }
}
/*
class JacksonClientStub[A](channel: ManagedChannel)
  extends GrpcClientStub[A, JavaTypeable](channel) with JacksonMethodDescriptorFactory

class PlayJsonClientStub[A](channel: ManagedChannel)
  extends GrpcClientStub[A, Format](channel) with PlayJsonMethodDescriptorFactory*/

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