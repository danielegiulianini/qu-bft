package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials, TlsChannelCredentials}
import qu.AsyncGrpcClientStub.{JwtJacksonClientStubAsync, UnauthenticatedJacksonClientStubAsync}
import qu.auth.Token

import scala.concurrent.ExecutionContext

trait AbstractRecipientInfo {
  def ip: String

  def port: Int
}

case class RecipientInfo(ip: String, port: Int) extends AbstractRecipientInfo

object RecipientInfo {
  //could be a method of RecipientInfo
  def id(serverInfo: AbstractRecipientInfo): String = serverInfo.ip + ":" + serverInfo.port
}

//equivalent of multiple applys
object StubFactories {
  //here key is ignored

  type StubFactory[Transportable[_]] = (String, Int, ExecutionContext) => AsyncGrpcClientStub[Transportable] //or def stubFactory[Transportable[_]](ip:String, port:Int):GrpcClientStub[Transportable] = ???

  val inNamedProcessJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) => {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonClientStubAsync(InProcessChannelBuilder.forName(ip + ":" + port).build())(ec)
  }

  /*def inNamedProcessJacksonStubFactory(ip:String, port:Int): UnauthenticatedJacksonClientStub = {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build())
  }*/

  val unencryptedDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) =>
    new UnauthenticatedJacksonClientStubAsync(Grpc.newChannelBuilder(ip + ":" + port,
      TlsChannelCredentials.create())
      .build)(ec)

  val tlsDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) =>
    new UnauthenticatedJacksonClientStubAsync(InProcessChannelBuilder.forName(ip + ":" + port)
      .build)(ec)

  type AuthenticatedStubFactory[Transportable[_]] = (Token, String, Int, ExecutionContext) => JwtAsyncGrpcClientStub[Transportable]

  val inProcessJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port, ec) =>
    new JwtJacksonClientStubAsync(InProcessChannelBuilder.forName(ip + ":" + port).build, token)(ec)

  val distributedJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port, ec) =>
    new JwtJacksonClientStubAsync(Grpc.newChannelBuilder(ip + ":" + port, InsecureChannelCredentials.create())
      .build, token)(ec)

  //analogous but with tls...
  //...

}


/*object ChannelFactories {

  //funzione riusabile da stringa a channel
  type ChannelFactory = RecipientInfo => Channel

  def insecureDistributedChannelBuilder(serverInfo: RecipientInfo) =
    Grpc.newChannelBuilder(serverInfo.ip + ":" + serverInfo.port, TlsChannelCredentials.create()).build()

  def tlsDistributedChannelBuilder(serverInfo: RecipientInfo) =
    Grpc.newChannelBuilder(serverInfo.ip, TlsChannelCredentials.create())
}
*/


/*object Shared {
  trait WithIp{ def ip: String }
  trait WithPort{def port: Int}
  trait WithSocket extends WithIp with WithPort

  trait WithHmacKey {def keySharedWithMe: String}
  case class RecipientInfo(ip: String, port: Int, keySharedWithMe: String) extends WithHmacKey with WithSocket
}*/
