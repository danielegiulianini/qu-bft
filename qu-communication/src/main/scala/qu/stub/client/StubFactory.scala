package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.{Grpc, InsecureChannelCredentials, ManagedChannel, ManagedChannelBuilder, TlsChannelCredentials}
import qu.{AbstractRecipientInfo, RecipientInfo}
import qu.RecipientInfo.id
import qu.auth.Token

import scala.concurrent.ExecutionContext

//abstract factory method
trait StubFactory3[Transportable[_]] {
  def inNamedProcessStub(ip: String, port: Int)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def inNamedProcessStub(recInfo: AbstractRecipientInfo)
                        (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(ip: String, port: Int)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

  def unencryptedDistributedStub(recInfo: AbstractRecipientInfo)
                                (implicit ec: ExecutionContext): AsyncClientStub[Transportable]

}

trait AuthenticatedStubFactory3[Transportable[_]] {
  def inNamedProcessJwtStub(token: Token, ip: String, port: Int)
                           (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def inNamedProcessJwtStub(token: Token, recInfo: AbstractRecipientInfo)
                           (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def unencryptedDistributedJwtStub(token: Token, ip: String, port: Int)
                                   (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

  def unencryptedDistributedJwtStub(token: Token, recInfo: AbstractRecipientInfo)
                                   (implicit ec: ExecutionContext): JwtAsyncClientStub[Transportable]

}

class JacksonStubFactory extends StubFactory3[JavaTypeable] {
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
  /*new UnauthenticatedJacksonAsyncClientStub(Grpc.newChannelBuilder(id(recInfo),
    TlsChannelCredentials.create()).build)*/
    new UnauthenticatedJacksonAsyncClientStub(Grpc.newChannelBuilder(id(recInfo),
      InsecureChannelCredentials.create()).build)
}

class JacksonAuthenticatedStubFactory extends AuthenticatedStubFactory3[JavaTypeable] {
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

/*
//equivalent of multiple applys
object StubFactories {

  type StubFactory[Transportable[_]] = (String, Int, ExecutionContext) => AsyncClientStub[Transportable] //or def stubFactory[Transportable[_]](ip:String, port:Int):GrpcClientStub[Transportable] = ???

  trait StubFactory2 {

  }

  val inNamedProcessJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) => {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonAsyncClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build())(ec)
  }

  def inNamedProcessJacksonStubFactory(ip: String, port: Int)(implicit ec: ExecutionContext): UnauthenticatedJacksonAsyncClientStub = {
    //could also use for address/for port
    //could validate with InetAddress
    new UnauthenticatedJacksonAsyncClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build())
  }

  val unencryptedDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) =>
    new UnauthenticatedJacksonAsyncClientStub(Grpc.newChannelBuilder(ip + ":" + port,
      TlsChannelCredentials.create())
      .build)(ec)

  val tlsDistributedJacksonStubFactory: StubFactory[JavaTypeable] = (ip, port, ec) =>
    new UnauthenticatedJacksonAsyncClientStub(InProcessChannelBuilder.forName(ip + ":" + port)
      .build)(ec)

  type AuthenticatedStubFactory[Transportable[_]] = (Token, String, Int, ExecutionContext) => JwtAsyncClientStub[Transportable]

  val inProcessJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port, ec) =>
    new JwtJacksonAsyncClientStub(InProcessChannelBuilder.forName(ip + ":" + port).build, token)(ec)

  val distributedJacksonJwtStubFactory: AuthenticatedStubFactory[JavaTypeable] = (token, ip, port, ec) =>
    new JwtJacksonAsyncClientStub(Grpc.newChannelBuilder(ip + ":" + port, InsecureChannelCredentials.create())
      .build, token)(ec)

  //analogous but with tls...
  //...

}
*/

/*object ChannelFactories {

  //funzione riusabile da stringa a channel
  type ChannelFactory = RecipientInfo => Channel

  def insecureDistributedChannelBuilder(serverInfo: RecipientInfo) =
    Grpc.newChannelBuilder(serverInfo.ip + ":" + serverInfo.port, TlsChannelCredentials.create()).build()

  def tlsDistributedChannelBuilder(serverInfo: RecipientInfo) =
    Grpc.newChannelBuilder(serverInfo.ip, TlsChannelCredentials.create())
}
*/

