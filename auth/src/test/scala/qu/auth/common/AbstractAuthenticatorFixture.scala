package qu.auth.common

import org.scalatest.{BeforeAndAfterEach, Suite}
import qu.auth.Role

import java.io.IOException

trait AbstractAuthenticatorFixture extends BeforeAndAfterEach {

  self: Suite =>

  import qu.auth.User

  val giovanni = new User("Giovanni Ciatto", "gciatto", Role.CLIENT)
  val andrea = new User("Andrea Omicini", "aomicini", Role.CLIENT)
  val stefano = new User("null", "stemar", Role.CLIENT)
  val noUser = new User(null, null, Role.CLIENT)
  val noPassword = new User(null, "someone", Role.CLIENT)

  var authenticator: Authenticator = _

  def createAuthenticator(): Authenticator

  override def beforeEach(): Unit = {
    beforeCreatingAuthenticator()
    authenticator = createAuthenticator()
    authenticator.register(giovanni)
    authenticator.register(andrea)
    authenticator.register(stefano)
    super.beforeEach()
  }

  override def afterEach(): Unit = {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally {
      shutdownAuthenticator()
    }
  }

  protected def shutdownAuthenticator(): Unit

  @throws[IOException]
  protected def beforeCreatingAuthenticator(): Unit
}
