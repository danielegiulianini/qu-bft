package qu

import auth.Constants
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc._
import qu.GrpcClientStub.{methodName, serviceName}
import scalapb.grpc.ClientCalls

import scala.concurrent.Future

//import that declares specific dependency

//a stub reusable between client and server sides
abstract class GrpcClientStub[Transferable[_]](val chan: ManagedChannel)
  extends MethodDescriptorFactory[Transferable] with MarshallerFactory[Transferable] with Shutdownable {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send2[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):
  Future[OutputT] = {
    //todo must add timeout
    val md = generateMethodDescriptor5[InputT, OutputT](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }

  override def shutdown(): Unit = chan.shutdown()
}


abstract class JwtGrpcClientStub[Transferable[_]](override val chan: ManagedChannel, val token: String)
  extends GrpcClientStub[Transferable](chan) {
  override val callOptions = CallOptions.DEFAULT.withCallCredentials(new AuthenticationCallCredentials(token))
}

import java.util.concurrent.Executor


//class qu.AuthenticationCallCredentials(var token: String) extends CallCredentials {

//this is used client side only?
class AuthenticationCallCredentials(var value: String) extends CallCredentials {
  override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo, executor: Executor, metadataApplier: CallCredentials.MetadataApplier): Unit = {
    executor.execute(() => {
      def foo() = {
        try {
          val headers = new Metadata
          headers.put(Constants.AUTHORIZATION_METADATA_KEY, String.format("%s %s", Constants.BEARER_TYPE, value))
          metadataApplier.apply(headers)
        } catch {
          case e: Throwable =>
            metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
        }
      }

      foo()
    })
  }

  override def thisUsesUnstableApi(): Unit = {
    // noop
  }
}


object GrpcClientStub {


  //decide where to inject (are grpc-specific constants)
  /*val methodName = "request"
  val serviceName = "io.grpc.KeyValueService"*/

  val methodName = QuServiceDescriptors.OPERATION_REQUEST_METHOD_NAME
  val serviceName = QuServiceDescriptors.SERVICE_NAME

  class UnauthenticatedJacksonClientStub(channel: ManagedChannel)
    extends GrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory


  class JwtJacksonClientStub(channel: ManagedChannel, token: String)
    extends JwtGrpcClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory

  //todo server specific, must go elsewhere
  /*def JacksonClientStubFactory: RecipientInfo => UnauthenticatedJacksonClientStub = serverInfo => new UnauthenticatedJacksonClientStub(
    ManagedChannelBuilder.forAddress(serverInfo.ip, serverInfo.port).build)*/

  //esempio di metodo di conversione degli impliciti (send2 li richiede, prova no)
  /*def prova[T, U]() = {
    val a = new UnauthenticatedJacksonClientStub(null)
    a.send2[Request[T, U], Response[Option[T]]](new Request[T, U](null, null))
  }*/
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