package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import org.scalatest.flatspec.FixtureAnyFlatSpecLike
import org.scalatest._
import qu.StubFactories.{inNamedProcessJacksonStubFactory, inProcessJacksonJwtStubFactory}
import qu.auth.Constants


// 'BeforeAndAfterEach' scalatest pattern (from:
// https://www.scalatest.org/user_guide/sharing_fixtures#withFixtureNoArgTest) as:
// 1. most or all tests need the same fixture
// 2. ix in a before-and-after trait when you want an aborted suite, not a failed test, if the fixture code fails.
trait AuthStubFixture extends BeforeAndAfterEach {
  this: Suite =>
  //a protected val to reuse stub with different recipient servers
  protected val serverInfo: RecipientInfo

  //lazy val for dealing with initialization issues
  lazy val authStub: JwtGrpcClientStub[JavaTypeable] = inProcessJacksonJwtStubFactory(getJwt,
    serverInfo.ip,
    serverInfo.port)

  private def getJwt: String = {
    Jwts.builder.setSubject("GreetingClient").signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
  }

  override def beforeEach() {
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach(): Unit = {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally authStub.shutdown()
  }
}

trait UnAuthStubFixture extends BeforeAndAfterEach {
  this: Suite =>
  protected val serverInfo: RecipientInfo

  lazy val unAuthStub = inNamedProcessJacksonStubFactory(serverInfo.ip, serverInfo.port)

  //todo: understand if needed or not.
  override def beforeEach() {
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach(): Unit = {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally unAuthStub.shutdown()
  }
}


/*
trait AsyncStub(Fixture) extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest) = {
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally buffer.clear()
  }
}*/