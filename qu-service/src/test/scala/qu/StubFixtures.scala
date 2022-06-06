package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.flatspec.FixtureAnyFlatSpecLike
import org.scalatest._
import qu.StubFactories.{inNamedProcessJacksonStubFactory, inProcessJacksonJwtStubFactory}
import qu.auth.Constants


trait AuthStubFixture extends BeforeAndAfterAll {
  this: AsyncTestSuite =>

  //lazy val for dealing with initialization issues
  lazy val authStub: JwtGrpcClientStub[JavaTypeable] = {
    println("creating authstub")

    inProcessJacksonJwtStubFactory(getJwt,
      serverInfo.ip,
      serverInfo.port)
  }

  //afterAll takes care of async code too (see https://github.com/scalatest/scalatest/issues/953)
  override def afterAll(): Unit = {
    println("shutting down ...")
    authStub.shutdown()
  }

  //a protected val to make stub reusable with different recipient servers
  protected val serverInfo: RecipientInfo
  protected val clientId: String

  private def getJwt: String = {
    Jwts.builder.setSubject(clientId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
  }
}

trait UnAuthStubFixture extends BeforeAndAfterAll {
  this: Suite =>


  lazy val unAuthStub = {
    println("creating unauthstub")
    inNamedProcessJacksonStubFactory(serverInfo.ip, serverInfo.port)
  }

  override def afterAll(): Unit = {
    println("shutting down ...")
    unAuthStub.shutdown()
  }

  protected val serverInfo: RecipientInfo
}