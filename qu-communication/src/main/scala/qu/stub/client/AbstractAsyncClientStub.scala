package qu.stub.client

import io.grpc._
import presentation.{MarshallerFactory, MethodDescriptorFactory}
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu._
import qu.auth.Token
import scalapb.grpc.ClientCalls

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}


/**
 * An asynchronous, unauthenticated, client stub reusable between client and server sides sending messages
 * without authorization metadata.
 * @tparam Transportable the higher-kinded type of the strategy responsible for messages (de)serialization.
 */
trait AsyncClientStub[Transportable[_]] {
  def send[InputT: Transportable, OutputT: Transportable](toBeSent: InputT):Future[OutputT]
}

abstract class AbstractAsyncClientStub[Transportable[_]](val chan: ManagedChannel)(implicit executor: ExecutionContext)
  extends AsyncClientStub[Transportable] with MethodDescriptorFactory[Transportable] with MarshallerFactory[Transportable] with Shutdownable {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send[InputT: Transportable, OutputT: Transportable](toBeSent: InputT):
  Future[OutputT] = {

    val md = generateMethodDescriptor[InputT, OutputT](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }

  override def shutdown(): Future[Unit] = Future {
    chan.shutdown()
    chan.awaitTermination(10, TimeUnit.SECONDS)
  }

  override def isShutdown: Boolean = chan.isShutdown
}

