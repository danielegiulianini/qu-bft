package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc._
import presentation.{MarshallerFactory, MethodDescriptorFactory}
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu._
import qu.auth.Token
import scalapb.grpc.ClientCalls

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

//a stub reusable between client and server sides
abstract class AsyncClientStub[Transferable[_]](val chan: ManagedChannel)(implicit executor: ExecutionContext)
  extends MethodDescriptorFactory[Transferable] with MarshallerFactory[Transferable] with Shutdownable {

  protected val callOptions: CallOptions = CallOptions.DEFAULT

  def send[InputT: Transferable, OutputT: Transferable](toBeSent: InputT):
  Future[OutputT] = {

    //todo must add timeout
    val md = generateMethodDescriptor[InputT, OutputT](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME)
    ClientCalls.asyncUnaryCall(chan, md, callOptions, toBeSent)
  }

  override def shutdown(): Future[Unit] = Future {
    println("shutting down channel " + this + "... (calling thread:" + Thread.currentThread().getName() + ")")
    chan.shutdown()
    chan.awaitTermination(30, TimeUnit.SECONDS)
    println("channel " + this + "  shut down !" + chan.isShutdown)

  }

  override def isShutdown: Boolean = chan.isShutdown
}

