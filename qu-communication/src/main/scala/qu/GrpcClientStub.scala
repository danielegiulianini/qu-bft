package qu

import auth.{Constants, Token}
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc._
import qu.GrpcClientStub.{methodName, serviceName}
import scalapb.grpc.ClientCalls

import java.util.concurrent.TimeUnit
import scala.concurrent.Future

//a stub reusable between client and server sides
abstract class GrpcClientStub[Transferable[_]](val chan: ManagedChannel)
  extends MethodDescriptorFactory[Transferable] with MarshallerFactory[Transferable] with Shutdownable {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):
  Future[OutputT] = {

    //todo must add timeout
    val md = generateMethodDescriptor5[InputT, OutputT](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }

  override def shutdown(): Unit = {
    chan.shutdown()
    chan.awaitTermination(1000, TimeUnit.SECONDS)
  }
}


abstract class JwtGrpcClientStub[Transferable[_]](override val chan: ManagedChannel, val token: Token)
  extends GrpcClientStub[Transferable](chan) {
  override val callOptions = CallOptions.DEFAULT.withCallCredentials(new AuthenticationCallCredentials(token))
}

import java.util.concurrent.Executor


//this is used client side only?
class AuthenticationCallCredentials(var token: Token) extends CallCredentials {
  override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
                                    executor: Executor,
                                    metadataApplier: CallCredentials.MetadataApplier): Unit = {
    executor.execute(() => {
      def fillHeadersWithToken() = {
        try {
          val headers = new Metadata
          headers.put(Constants.AUTHORIZATION_METADATA_KEY, String.format("%s %s", Constants.BEARER_TYPE, token.username))
          metadataApplier.apply(headers)
        } catch {
          case e: Throwable =>
            metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
        }
      }

      fillHeadersWithToken()
    })
  }

  override def thisUsesUnstableApi(): Unit = {}
}


object GrpcClientStub {

  val methodName = QuServiceDescriptors.OPERATION_REQUEST_METHOD_NAME
  val serviceName = QuServiceDescriptors.SERVICE_NAME

  class UnauthenticatedJacksonClientStub(channel: ManagedChannel)
    extends GrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory
      with CachingMethodDescriptorFactory[JavaTypeable]

  class JwtJacksonClientStub(channel: ManagedChannel, token: Token)
    extends JwtGrpcClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory
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