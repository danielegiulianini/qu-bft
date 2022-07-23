package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannelBuilder
import io.grpc.inprocess.InProcessChannelBuilder
import qu.SocketAddress.id
import qu.auth.Token
import qu.{AbstractSocketAddress, SocketAddress}

import scala.concurrent.ExecutionContext


/**
 * An implementation of [[qu.stub.client.AuthenticatedStubFactory]] leveraging Jackson (so, JSON)
 * as (de)serialization technology.
 */
class JacksonAuthenticatedStubFactory extends AuthenticatedStubFactory[JavaTypeable] {
  override def inNamedProcessJwtStub(token: Token, ip: String, port: Int)
                                    (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    inNamedProcessJwtStub(token, SocketAddress(ip, port))

  override def inNamedProcessJwtStub(token: Token, recInfo: AbstractSocketAddress)
                                    (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    new JacksonJwtAsyncClientStub(InProcessChannelBuilder.forName(id(recInfo)).build, token)

  override def unencryptedDistributedJwtStub(token: Token, ip: String, port: Int)
                                            (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    unencryptedDistributedJwtStub(token, SocketAddress(ip, port))

  override def unencryptedDistributedJwtStub(token: Token, recInfo: AbstractSocketAddress)
                                            (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    new JacksonJwtAsyncClientStub(ManagedChannelBuilder.forAddress(recInfo.ip, recInfo.port).usePlaintext().build //Grpc.newChannelBuilder(id(recInfo),InsecureChannelCredentials.create()).build
      , token
    ) //TlsChannelCredentials.create()).build, token)
}
