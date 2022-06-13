package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc._
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.auth.Token
import scalapb.grpc.ClientCalls

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

//a stub reusable between client and server sides
abstract class AsyncGrpcClientStub[Transferable[_]](val chan: ManagedChannel)(implicit executor: ExecutionContext)
  extends MethodDescriptorFactory[Transferable] with MarshallerFactory[Transferable] with Shutdownable {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):
  Future[OutputT] = {

    //todo must add timeout
    val md = generateMethodDescriptor5[InputT, OutputT](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }

  override def shutdown(): Future[Unit] = Future {
    chan.shutdown()
    chan.awaitTermination(1000, TimeUnit.SECONDS)
  }

  override def isShutdown: Boolean = chan.isShutdown
}





object AsyncGrpcClientStub {

  class UnauthenticatedJacksonClientStubAsync(channel: ManagedChannel)(implicit executor: ExecutionContext)
    extends AsyncGrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory
      with CachingMethodDescriptorFactory[JavaTypeable]

  class JwtJacksonClientStubAsync(channel: ManagedChannel, token: Token)(implicit executor: ExecutionContext)
    extends JwtAsyncGrpcClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory
      with CachingMethodDescriptorFactory[JavaTypeable]

}


/*
class JacksonClientStub[A](channel: ManagedChannel)
  extends qu.GrpcClientStub[A, JavaTypeable](channel) with JacksonMethodDescriptorFactory

class PlayJsonClientStub[A](channel: ManagedChannel)
  extends qu.GrpcClientStub[A, Format](channel) with PlayJsonMethodDescriptorFactory*/

//example of use:
//class JacksonCLientStub extends qu.GrpcClientStub(null) with JacksonMethodDescriptorFactory
//with cake pattern:
//class JacksonCLientStub extends qu.GrpcClientStub(null) with JacksonMethodDescriptorFactory {
//val a = new A (declared in qu.GrpcClientStub); val b= new B (declared in qu.GrpcClientStub))
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