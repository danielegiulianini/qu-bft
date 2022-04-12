import com.fasterxml.jackson.module.scala.JavaTypeable
import com.google.common.util.concurrent.ListenableFuture
import io.grpc.stub.ClientCalls
import io.grpc.{CallOptions, ManagedChannel, MethodDescriptor}
import qu.protocol.Messages.{Request, Response}
import qu.protocol.{Messages, MethodDescriptorFactory}


//this is a grpc ClientStub
abstract class ClientStub[T](var chan: ManagedChannel) extends MethodDescriptorFactory {

  //decide where to inject
  val methodName = "todo"
  val serviceName = "todo"

  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                    (implicit enc: Marshallable[Request[T, U]],
                                     enc3: Marshallable[Response[T]],
                                     enc2: Marshallable[T], dec: Marshallable[U]):
  MethodDescriptor[Messages.Request[T, U], Messages.Response[T]]

  def send[T, U](op: Messages.Request[T, U])
                (implicit enc: Marshallable[T], dec: Marshallable[U],
                 marshallable: Marshallable[Request[T, U]], marshallable3: Marshallable[Response[T]]):
    ListenableFuture[Messages.Response[T]] = {
    //todo flyWeight pattern to be used here
    val md2 = generateMethodDescriptor[T, U](methodName, serviceName) //this.methodDescriptors.computeIfAbsent(op.getClass, (c: Class[_]) => KvGson.generateMethodDescriptor(classOf[KvGson.Wrapping[_, _]]))
    ClientCalls.futureUnaryCall(chan.newCall(md2, CallOptions.DEFAULT), op)
  }
}

//example of use: class JacksonCLientStub extends ClientStub with JacksonMethodDescriptorFactory
