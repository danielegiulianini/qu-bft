package qu.auth.common

import org.scalatest.Suite

trait LocalAuthenticatorFixture extends AbstractAuthenticatorFixture {
  self: Suite =>

  override def createAuthenticator() = new LocalAuthenticator()

  override def beforeCreatingAuthenticator(): Unit = {
    authenticator = new LocalAuthenticator()
  }

  override def shutdownAuthenticator(): Unit = {
  }
}
