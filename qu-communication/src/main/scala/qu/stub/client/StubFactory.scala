package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials, ManagedChannel, ManagedChannelBuilder, TlsChannelCredentials}
import qu.{AbstractRecipientInfo, RecipientInfo}
import qu.RecipientInfo.id
import qu.auth.Token

import scala.concurrent.ExecutionContext

//abstract factory method
trait StubFactory[Transportable[_]] {
  def inNamedProcessStub(ip: String, port: Int)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def inNamedProcessStub(recInfo: AbstractRecipientInfo)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(ip: String, port: Int)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(recInfo: AbstractRecipientInfo)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

}







