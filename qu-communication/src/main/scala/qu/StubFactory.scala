package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials, TlsChannelCredentials}
import qu.GrpcClientStub.{JwtJacksonClientStub, UnauthenticatedJacksonClientStub}


//equivalent of multiple applys
object StubFactories {
  //here key is ignored

  type StubFactory[Transportable[_]] = (String, Int) => GrpcClientStub[Transportable]  //or def stubFactory[Transportable[_]](ip:String, port:Int):GrpcClientStub[Transportable] = ???

  val inNamedProcessJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port) => {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build())
  }

  def inNamedProcessJacksonStubFactory(ip:String, port:String): UnauthenticatedJacksonClientStub = {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build())
  }

  val unencryptedDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port) =>
    new UnauthenticatedJacksonClientStub(Grpc.newChannelBuilder(ip + ":" + port,
      TlsChannelCredentials.create())
      .build)

  val tlsDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port) =>
    new UnauthenticatedJacksonClientStub(InProcessChannelBuilder.forName(ip + ":" + port)
      .build)

  type SenderToken = String
  type AuthenticatedStubFactory[Transportable[_]] = (SenderToken, String, Int) => JwtGrpcClientStub[Transportable]

  val inProcessJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port) =>
    new JwtJacksonClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build, token)

  val distributedJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port) =>
    new JwtJacksonClientStub(Grpc.newChannelBuilder(ip + ":" + port, InsecureChannelCredentials.create())
      .build, token)

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
