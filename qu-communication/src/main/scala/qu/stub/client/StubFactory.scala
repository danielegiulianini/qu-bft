package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials, ManagedChannel, ManagedChannelBuilder, TlsChannelCredentials}
import qu.{AbstractSocketAddress, SocketAddress}
import qu.SocketAddress.id
import qu.auth.Token

import scala.concurrent.ExecutionContext

//abstract factory method
trait StubFactory[Transportable[_]] {
  def inNamedProcessStub(ip: String, port: Int)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def inNamedProcessStub(recInfo: AbstractSocketAddress)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(ip: String, port: Int)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(recInfo: AbstractSocketAddress)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

}







