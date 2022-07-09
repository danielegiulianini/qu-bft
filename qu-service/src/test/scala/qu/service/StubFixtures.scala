package qu.service

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest._
import qu.SocketAddress
import qu.auth.Token
import qu.auth.common.Constants
import qu.stub.client.{JacksonAuthenticatedStubFactory, JacksonStubFactory}


trait AuthStubFixture extends BeforeAndAfterAll {
  this: AsyncTestSuite => //AsyncTestSuite includes implicit executor needed

  //lazy val for dealing with initialization issues
  lazy val authStub =
    new JacksonAuthenticatedStubFactory().inNamedProcessJwtStub(getJwt,
      serverInfo)

  //afterAll takes care of async code too (see https://github.com/scalatest/scalatest/issues/953)
  override def afterAll(): Unit = {
    authStub.shutdown()
  }

  //a protected val to make stub reusable with different recipient servers
  protected val serverInfo: SocketAddress
  protected val clientId: String

  private def getJwt: Token =
    Token(Jwts.builder.setSubject(clientId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact)
}


trait UnAuthStubFixture extends BeforeAndAfterAll {
  this: AsyncTestSuite =>

  lazy val unAuthStub = {
    new JacksonStubFactory().inNamedProcessStub(serverInfo)
  }

  override def afterAll(): Unit = {
    unAuthStub.shutdown()
  }

  protected val serverInfo: SocketAddress

}