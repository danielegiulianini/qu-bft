package qu.stub.client

import qu.{AbstractSocketAddress, SocketAddress}

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







