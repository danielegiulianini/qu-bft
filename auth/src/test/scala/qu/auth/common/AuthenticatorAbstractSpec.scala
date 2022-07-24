package qu.auth.common

import org.scalatest.Suite
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers.{an, be}
import qu.auth.{Credentials, Token, User}

trait AuthenticatorAbstractSpec extends AnyFunSpec {

  self: Suite with AbstractAuthenticatorFixture =>

  describe("An authenticator") {
    describe("when registering and there is a conflict") {
      it("must throw ConflictException to the caller") {
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(andrea)
        }
      }
    }
    describe("when registering and there is another conflict") {
      it("must throw ConflictException to the caller") {
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(giovanni)
        }
      }
    }
    describe("when registering and there is a further conflict") {
      it("must throw ConflictException to the caller") {
        an[ConflictException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(stefano)
        }
      }
    }
    describe("when registering a user without a username") {
      it("must throw BadContentException to the caller") {
        an[BadContentException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(noUser)
        }
      }
    }
    describe("when registering a user without password") {
      it("must throw BadContentException to the caller") {
        an[BadContentException] should be thrownBy { // Ensure a particular exception type is thrown
          authenticator.register(noPassword)
        }
      }
    }
  }


  describe("An authenticator") {
    describe("when trying to authorize a user with a wrong username") {
      it("must throw WrongCredentialsException to the caller") {
        for (user <- List(giovanni, andrea, stefano)) {
          an[WrongCredentialsException] should be thrownBy {
            authenticator.authorize(credentialsOf(user.copy(username = user.username + "2")))
          }
        }
      }
    }
    describe("when trying to authorize a user with a wrong password") {
      it("must throw WrongCredentialsException to the caller") {
        for (user <- List(giovanni, andrea, stefano)) {
          an[WrongCredentialsException] should be thrownBy {
            authenticator.authorize(credentialsOf(user.copy(password = user.password + "-")))
          }
        }
      }
    }
    describe("when trying to authorize a user without username") {
      it("must throw BadContentException to the caller") {
        an[BadContentException] should be thrownBy {
          authenticator.authorize(credentialsOf(noUser))
        }
      }
    }
    describe("when trying to authorize a user without password") {
      it("must throw BadContentException to the caller") {
        an[BadContentException] should be thrownBy {
          authenticator.authorize(credentialsOf(noPassword))
        }
      }
    }
  }


  def credentialsOf(user: User) = new Credentials(user.username, user.password)

  def tokenOf(user: User) = new Token(user.username, user.role)
}
