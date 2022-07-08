package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannelBuilder
import io.grpc.inprocess.InProcessChannelBuilder
import qu.RecipientInfo.id
import qu.auth.Token
import qu.{AbstractRecipientInfo, RecipientInfo}

import scala.concurrent.ExecutionContext

class JacksonAuthenticatedStubFactory extends AuthenticatedStubFactory[JavaTypeable] {
  override def inNamedProcessJwtStub(token: Token, ip: String, port: Int)
                                    (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    inNamedProcessJwtStub(token, RecipientInfo(ip, port))

  override def inNamedProcessJwtStub(token: Token, recInfo: AbstractRecipientInfo)
                                    (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    new JwtJacksonAsyncClientStub(InProcessChannelBuilder.forName(id(recInfo)).build, token)

  override def unencryptedDistributedJwtStub(token: Token, ip: String, port: Int)
                                            (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    unencryptedDistributedJwtStub(token, RecipientInfo(ip, port))

  override def unencryptedDistributedJwtStub(token: Token, recInfo: AbstractRecipientInfo)
                                            (implicit ec: ExecutionContext): JwtAsyncClientStub[JavaTypeable] =
    new JwtJacksonAsyncClientStub(ManagedChannelBuilder.forAddress(recInfo.ip, recInfo.port).usePlaintext().build //Grpc.newChannelBuilder(id(recInfo),InsecureChannelCredentials.create()).build
      , token
    ) //TlsChannelCredentials.create()).build, token)
}
