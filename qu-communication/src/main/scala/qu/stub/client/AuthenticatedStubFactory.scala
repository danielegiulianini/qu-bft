package qu.stub.client

import qu.AbstractSocketAddress
import qu.auth.Token

import scala.concurrent.ExecutionContext

trait AuthenticatedStubFactory[Transportable[_]] {
  def inNamedProcessJwtStub(token: Token, ip: String, port: Int)
                           (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def inNamedProcessJwtStub(token: Token, recInfo: AbstractSocketAddress)
                           (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def unencryptedDistributedJwtStub(token: Token, ip: String, port: Int)
                                   (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def unencryptedDistributedJwtStub(token: Token, recInfo: AbstractSocketAddress)
                                   (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

}
