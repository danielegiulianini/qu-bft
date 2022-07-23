package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.Server
import io.grpc.inprocess.InProcessServerBuilder
import presentation.CachingMethodDescriptorFactory
import qu.SocketAddress.id
import qu.UsageExampleCodeGrpcUnaware.{quReplica1Info, thresholds}
import qu.service.AbstractGrpcQuService.QuServiceBuilder
import qu.service.{AbstractGrpcQuService, JwtAuthorizationServerInterceptor}
import qu.service.quorum.JacksonBroadcastBroadcastServerPolicy
import qu.storage.ImmutableStorage


object UsageExampleCodeGrpcAware {

  import scala.concurrent.ExecutionContext.Implicits.global

  val quServer1SocketAddr: SocketAddress = SocketAddress(ip = "localhost", port = 1000)

  val quService: AbstractGrpcQuService[JavaTypeable, Int] = QuServiceBuilder(
    methodDescriptorFactory = new JacksonMethodDescriptorFactory with CachingMethodDescriptorFactory[JavaTypeable] {},
    policyFactory = JacksonBroadcastBroadcastServerPolicy[Int](_, _),
    thresholds = thresholds,
    quReplica1Info,
    obj = 0,
    storage = ImmutableStorage[Int]()).build()


  val server: Server = InProcessServerBuilder
    .forName(id(quReplica1Info))
    .intercept(new JwtAuthorizationServerInterceptor())
    .addService(quService)
    .build


}
