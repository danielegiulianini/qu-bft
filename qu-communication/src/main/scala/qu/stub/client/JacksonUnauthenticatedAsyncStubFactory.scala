package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials}
import qu.SocketAddress.id
import qu.{AbstractSocketAddress, SocketAddress}

import scala.concurrent.ExecutionContext


/**
 * An implementation of [[qu.stub.client.UnauthenticatedAsyncStubFactory]] leveraging Jackson (so, JSON)
 * as (de)serialization technology.
 * */
class JacksonUnauthenticatedAsyncStubFactory extends UnauthenticatedAsyncStubFactory[JavaTypeable] {
  override def inNamedProcessStub(ip: String, port: Int)
                                 (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    inNamedProcessStub(SocketAddress(ip, port))

  override def inNamedProcessStub(recInfo: AbstractSocketAddress)
                                 (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    new JacksonUnauthenticatedAsyncClientStub(InProcessChannelBuilder.forName(id(recInfo)).build())

  override def unencryptedDistributedStub(ip: String, port: Int)
                                         (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    unencryptedDistributedStub(SocketAddress(ip, port))

  override def unencryptedDistributedStub(recInfo: AbstractSocketAddress)
                                         (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    new JacksonUnauthenticatedAsyncClientStub(Grpc.newChannelBuilder(id(recInfo),
      InsecureChannelCredentials.create()).build) //    TlsChannelCredentials.create()).build)
}
