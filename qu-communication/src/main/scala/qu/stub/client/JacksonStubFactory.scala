package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials}
import qu.SocketAddress.id
import qu.{AbstractSocketAddress, SocketAddress}

import scala.concurrent.ExecutionContext

class JacksonStubFactory extends StubFactory[JavaTypeable] {
  override def inNamedProcessStub(ip: String, port: Int)
                                 (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    inNamedProcessStub(SocketAddress(ip, port))

  override def inNamedProcessStub(recInfo: AbstractSocketAddress)
                                 (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    new UnauthenticatedJacksonAsyncClientStub(InProcessChannelBuilder.forName(id(recInfo)).build())

  override def unencryptedDistributedStub(ip: String, port: Int)
                                         (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    unencryptedDistributedStub(SocketAddress(ip, port))

  override def unencryptedDistributedStub(recInfo: AbstractSocketAddress)
                                         (implicit ec: ExecutionContext): AbstractAsyncClientStub[JavaTypeable] =
    new UnauthenticatedJacksonAsyncClientStub(Grpc.newChannelBuilder(id(recInfo),
      InsecureChannelCredentials.create()).build) //    TlsChannelCredentials.create()).build)
}
