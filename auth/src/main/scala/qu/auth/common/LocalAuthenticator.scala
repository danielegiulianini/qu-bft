//class not dependent on specific transport layer
package qu.auth.common

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import qu.auth.{Credentials, Role, Token, User}

/**
 * Local implementation of [[Authenticator]], so providing authentication/authorization APIs by means of a
 * pass-by-reference semantics.
 */
class LocalAuthenticator extends Authenticator {
  private var usersByUsername = Map[String, User]()

  @throws[BadContentException]
  @throws[ConflictException]
  def register(user: User): Unit = {
    if (user == null) throw BadContentException("User can't be null")
    if (user.username.isBlank) throw BadContentException("Invalid username: " + user.username)
    if (user.password.isBlank) throw BadContentException("No password provided for user: " + user.username)
    this.synchronized {

      if (usersByUsername.contains(user.username)) {
        //not logging exceptions to avoid "log and throw" anti-pattern
        throw ConflictException("Username already exists: " + user.username)
      }
      val toBeAdded = user.copy() // defensive copy
      usersByUsername = usersByUsername + (user.username -> toBeAdded)
    }
  }

  @throws[BadContentException]
  @throws[WrongCredentialsException]
  def authorize(credentials: Credentials): Token = {
    if (credentials == null) throw BadContentException("Credentials can't be null")
    if (credentials.username.isBlank) throw BadContentException("Missing user ID: " + credentials.username)
    if (credentials.password.isBlank) throw BadContentException("Missing password: " + credentials.password)
    val userId = credentials.username
    this.synchronized {
      val user: User = usersByUsername.get(userId).getOrElse(throw WrongCredentialsException("No such a user: " + userId))
      if (!credentials.password.equals(user.password)) throw WrongCredentialsException("Wrong credentials for user: " + userId)
      val encryptedUserId = Jwts.builder.setSubject(userId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
      val role = user.role
      new Token(encryptedUserId, if (role!=null) role else Role.CLIENT)
    }
  }
}
