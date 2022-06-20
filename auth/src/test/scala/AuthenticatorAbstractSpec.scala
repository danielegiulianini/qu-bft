import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.funspec.AnyFunSpec
import qu.auth.{Credentials, Role, Token, User}
import qu.auth.common.{Authenticator, ConflictException, WrongCredentialsException}

trait LocalAuthenticatorFixAbstractSpec extends AnyFunSpec {

  self: Suite with WithAuthenticator =>

  describe("An authenticator") {
    describe("when registering and something go wrong") {
      it("must throw the corresponding exception to the caller") {
        assertThrows(classOf[ConflictException], () => authenticator.register(andrea))
        assertThrows(classOf[ConflictException], () => authenticator.register(giovanni))
        assertThrows(classOf[ConflictException], () => authenticator.register(stefano))
        assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noUser))
        assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noPassword))
        assertThrows(classOf[IllegalArgumentException], () => authenticator.register(noEmail))
      }
    }

    def credentialsOf(user: User) = new Credentials(user.username, user.password)

    def tokenOf(user: User) = new Token(user.username, user.role)

    @throws[WrongCredentialsException]
    def testAuthorize(): Unit = {
      for (user <- List(giovanni, andrea, stefano)) {
        assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(user.copy(username = user.username + "2"))))
        assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(user.copy(password = user.password + "-"))))
      }
      assertThrows(classOf[IllegalArgumentException], () => authenticator.authorize(credentialsOf(noUser)))
      assertThrows(classOf[IllegalArgumentException], () => authenticator.authorize(credentialsOf(noPassword)))
      assertThrows(classOf[WrongCredentialsException], () => authenticator.authorize(credentialsOf(noEmail)))
    }
  }
}


