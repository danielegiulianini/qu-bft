import GrpcClientStub.{UnauthenticatedJacksonClientStub, methodName, serviceName}
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.{CallOptions, ManagedChannel, ManagedChannelBuilder, Metadata}
import Shared.RecipientInfo
import qu.protocol.{JacksonMethodDescriptorFactory, MarshallerFactory, MethodDescriptorFactory, TemporaryConstants}
import scalapb.grpc.ClientCalls

import scala.Predef.->
import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.model.ConcreteQuModel._

//a stub reusable between client and server sides
abstract class GrpcClientStub[Transferable[_]](val chan: ManagedChannel)
  extends MethodDescriptorFactory[Transferable] with MarshallerFactory[Transferable] {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send2[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):
  Future[OutputT] = {
    //must add timeout
    val md = generateMethodDescriptor5[InputT, OutputT](methodName, serviceName)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }
}


abstract class JwtGrpcClientStub[Marshallable[_]](override val chan: ManagedChannel, val token: String)
  extends GrpcClientStub[Marshallable](chan) {
  override val callOptions = CallOptions.DEFAULT.withCallCredentials(new AuthenticationCallCredentials(token))
}

import io.grpc.CallCredentials
import io.grpc.CallCredentials.MetadataApplier
import io.grpc.Status
import java.util.concurrent.Executor
import io.grpc.Context
import io.grpc.Metadata

object Constants {
  val JWT_SIGNING_KEY = "L8hHXsaQOUjk5rg7XPGv4eL36anlCrkMz8CJ0i/8E/0="
  val BEARER_TYPE = "Bearer"
  val AUTHORIZATION_METADATA_KEY: Metadata.Key[String] = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
  val CLIENT_ID_CONTEXT_KEY: Context.Key[String] = Context.key("clientId")
}

class Constants private() {
  throw new AssertionError
}
//class AuthenticationCallCredentials(var token: String) extends CallCredentials {

import io.grpc.CallCredentials
import io.grpc.CallCredentials.MetadataApplier
import io.grpc.Status
import java.util.concurrent.Executor

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
/*override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
                                  executor: Executor,
                                  metadataApplier: CallCredentials.MetadataApplier): Unit = {
  executor.execute(() => {
    def foo() = {
      try {
        val headers = new Metadata
        //headers.put(AuthenticationConstants.META_DATA_KEY, "Bearer " + token)
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
  // yes this is unstable :(
}*/
//}


object GrpcClientStub {


  //decide where to inject (are grpc-specific constants)
  /*val methodName = "request"
  val serviceName = "io.grpc.KeyValueService"*/
  val methodName = TemporaryConstants.METHOD_NAME
  val serviceName = TemporaryConstants.SERVICE_NAME

  class UnauthenticatedJacksonClientStub(channel: ManagedChannel)
    extends GrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory


  class JwtJacksonClientStub(channel: ManagedChannel, token: String)
    extends JwtGrpcClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory

  //todo server specific, must go elsewhere
  def JacksonClientStubFactory: RecipientInfo => UnauthenticatedJacksonClientStub = serverInfo => new UnauthenticatedJacksonClientStub(
    ManagedChannelBuilder.forAddress(serverInfo.ip, serverInfo.port).build)

  //esempio di metodo di conversione degli impliciti (send2 li richiede, prova no)
  def prova[T, U]() = {
    val a = new UnauthenticatedJacksonClientStub(null)
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