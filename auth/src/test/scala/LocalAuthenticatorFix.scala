import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import qu.auth.Role
import qu.auth.common.{Authenticator, LocalAuthenticator}

import java.io.IOException


trait LocalAuthenticatorFix extends WithAuthenticatorFix {
  self: Suite =>

  override var authenticator: Authenticator = new LocalAuthenticator()

  override def beforeCreatingAuthenticator(): Unit = {
  }

  override def shutdownAuthenticator(): Unit = {
  }
}
