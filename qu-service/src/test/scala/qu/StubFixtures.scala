package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.flatspec.FixtureAnyFlatSpecLike
import org.scalatest._
import qu.stub.client.StubFactories.{inNamedProcessJacksonStubFactory, inProcessJacksonJwtStubFactory}
import qu.auth.Token
import qu.auth.common.Constants
import qu.stub.client.JwtAsyncClientStub

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext


trait AuthStubFixture extends BeforeAndAfterAll {
  this: AsyncTestSuite =>

  def authStub2()(implicit ex:ExecutionContext) : JwtAsyncClientStub[JavaTypeable] =
    inProcessJacksonJwtStubFactory(getJwt,
    serverInfo.ip,
    serverInfo.port, ex)

  //lazy val for dealing with initialization issues
  /*lazy val authStub: JwtAsyncClientStub[JavaTypeable] =
    inProcessJacksonJwtStubFactory(getJwt,
      serverInfo.ip,
      serverInfo.port, e)*/

  //afterAll takes care of async code too (see https://github.com/scalatest/scalatest/issues/953)
  override def afterAll(): Unit = {
    authStub.shutdown()
  }

  //a protected val to make stub reusable with different recipient servers
  protected val serverInfo: RecipientInfo
  protected val clientId: String

  private def getJwt: Token = {
    Token(Jwts.builder.setSubject(clientId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact)
  }
}

trait UnAuthStubFixture extends BeforeAndAfterAll {
  this: Suite =>
  protected[this] implicit def e: ExecutionContext = implicitly

  lazy val unAuthStub = {
    inNamedProcessJacksonStubFactory(serverInfo.ip, serverInfo.port, e)
  }

  override def afterAll(): Unit = {
    unAuthStub.shutdown()
  }

  protected val serverInfo: RecipientInfo

}


/*
*
*
trait AuthStubFixture extends BeforeAndAfterAll {
  this: AsyncTestSuite =>

  def authStub2()(implicit ex:ExecutionContext) : JwtAsyncClientStub[JavaTypeable] =
    inProcessJacksonJwtStubFactory(getJwt,
    serverInfo.ip,
    serverInfo.port, ex)

  //lazy val for dealing with initialization issues
  /*lazy val authStub: JwtAsyncClientStub[JavaTypeable] =
    inProcessJacksonJwtStubFactory(getJwt,
      serverInfo.ip,
      serverInfo.port, e)*/

  //afterAll takes care of async code too (see https://github.com/scalatest/scalatest/issues/953)
  override def afterAll(): Unit = {
    authStub.shutdown()
  }

  //a protected val to make stub reusable with different recipient servers
  protected val serverInfo: RecipientInfo
  protected val clientId: String

  private def getJwt: Token = {
    Token(Jwts.builder.setSubject(clientId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact)
  }
}

trait UnAuthStubFixture extends BeforeAndAfterAll {
  this: Suite =>
  protected[this] implicit def e: ExecutionContext = implicitly

  lazy val unAuthStub = {
    inNamedProcessJacksonStubFactory(serverInfo.ip, serverInfo.port, e)
  }

  override def afterAll(): Unit = {
    unAuthStub.shutdown()
  }

  protected val serverInfo: RecipientInfo

}*/