import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatest.funspec.AnyFunSpec
import qu.auth.{Credentials, Role, Token, User}
import qu.auth.common.{Authenticator, BadContentException, ConflictException, WrongCredentialsException}
import org.scalatest._
import Assertions._
import org.scalatest.matchers.must.Matchers.{an, be}


trait AuthenticatorAbstractSpec extends AnyFunSpec {

  self: Suite with WithAuthenticatorFix =>

  describe("An authenticator") {
    describe("when registering and something go wrong") {
      it("must throw the corresponding exception to the caller") {
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(andrea)
        }
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(giovanni)
        }
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(stefano)
        }
        an[BadContentException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(noUser)
        }
        an[BadContentException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(noPassword)
        }
        an[BadContentException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(noEmail)
        }
      }
    }


    describe("An authenticator") {
      describe("when authorizing and something go wrong") {
        it("must throw the corresponding exception to the caller") {
          for (user <- List(giovanni, andrea, stefano)) {
            an[WrongCredentialsException] should be thrownBy {
              authenticator.authorize(credentialsOf(user.copy(username = user.username + "2")))
            }
            an[WrongCredentialsException] should be thrownBy {
              authenticator.authorize(credentialsOf(user.copy(password = user.password + "-")))
            }
          }
          an[BadContentException] should be thrownBy {
            authenticator.authorize(credentialsOf(noUser))
          }
        }
        an[BadContentException] should be thrownBy {
          authenticator.authorize(credentialsOf(noPassword))
        }
        an[BadContentException] should be thrownBy {
          authenticator.authorize(credentialsOf(noEmail))
        }
      }
    }
  }

  def credentialsOf(user: User) = new Credentials(user.username, user.password)

  def tokenOf(user: User) = new Token(user.username, user.role)
}


