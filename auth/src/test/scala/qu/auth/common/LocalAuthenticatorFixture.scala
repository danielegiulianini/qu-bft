package qu.auth.common

import org.scalatest.Suite

trait LocalAuthenticatorFixture extends AbstractAuthenticatorFixture {
  self: Suite =>

  override var authenticator: Authenticator = new LocalAuthenticator()

  override def beforeCreatingAuthenticator(): Unit = {
  }

  override def shutdownAuthenticator(): Unit = {
  }
}
