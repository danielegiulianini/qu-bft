package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials}
import qu.RecipientInfo.id
import qu.{AbstractRecipientInfo, RecipientInfo}

import scala.concurrent.ExecutionContext

class JacksonStubFactory extends StubFactory[JavaTypeable] {
  override def inNamedProcessStub(ip: String, port: Int)
                                 (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    inNamedProcessStub(RecipientInfo(ip, port))

  override def inNamedProcessStub(recInfo: AbstractRecipientInfo)
                                 (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    new UnauthenticatedJacksonAsyncClientStub(InProcessChannelBuilder.forName(id(recInfo)).build())

  override def unencryptedDistributedStub(ip: String, port: Int)
                                         (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    unencryptedDistributedStub(RecipientInfo(ip, port))

  override def unencryptedDistributedStub(recInfo: AbstractRecipientInfo)
                                         (implicit ec: ExecutionContext): AsyncClientStub[JavaTypeable] =
    new UnauthenticatedJacksonAsyncClientStub(Grpc.newChannelBuilder(id(recInfo),
      InsecureChannelCredentials.create()).build) //    TlsChannelCredentials.create()).build)
}
