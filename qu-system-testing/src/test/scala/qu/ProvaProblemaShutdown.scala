package qu

import com.google.protobuf.duration.Duration.defaultInstance.seconds
import io.grpc.CallOptions
import io.grpc.inprocess.InProcessChannelBuilder
import qu.QuServiceDescriptors.{OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME}
import qu.RecipientInfo.id
import qu.model.OHSUtilities
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.service.datastructures.RemoteCounterServer
import scalapb.grpc.ClientCalls

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.SECONDS

object ProvaProblemaShutdown extends App with OHSUtilities with ServersFixture {

  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val quServer = LocalQuServerCluster[Int](quServerIpPorts,
    keysByServer,
    thresholds,
    RemoteCounterServer.builder,
    InitialObject)

  val quServerInfo = RecipientInfo("localhost", 1001)
  val channel = InProcessChannelBuilder.forName(id(quServerInfo)).build

  val mdf = new JacksonMethodDescriptorFactory {}
  val md = mdf.generateMethodDescriptor[Int, String](OPERATION_REQUEST_METHOD_NAME, SERVICE_NAME)
  ClientCalls.asyncUnaryCall(channel, md, CallOptions.DEFAULT, 2).map (e => println(e))

  Future {
    channel.shutdown()
    channel.awaitTermination(3, SECONDS)
  }
  println("channel is Shutdown ?" + channel.isShutdown)
  val channel2 = InProcessChannelBuilder.forName(id(quServerInfo)).build
  ClientCalls.asyncUnaryCall(channel, md, CallOptions.DEFAULT, 2).map (e => println(e))

  channel2.shutdown()
}
