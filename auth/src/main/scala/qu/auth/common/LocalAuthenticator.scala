//class not dependent on specific transport layer
package qu.auth.common

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import qu.auth.{Credentials, Role, Token, User}

class LocalAuthenticator {
  private val usersByUsername = Map[String, User]()

  @throws[BadContentException]
  @throws[ConflictException]
  def register(user: User): Unit = {
    //I assume credentials can't be null, so I omit: if (user == null ||...
    if (user.username.isBlank) throw new BadContentException("Invalid username: " + user.username)
    if (user.password.isBlank) throw new BadContentException("No password provided for user: " + user.username)
    this.synchronized {
      if (usersByUsername.contains(user.username)) throw new ConflictException("Username already exists: " + user.username)
      val toBeAdded = user.copy() // defensive copy
      usersByUsername + (user.username -> toBeAdded)
    }
  }

  @throws[BadContentException]
  @throws[WrongCredentialsException]
  def authorize(credentials: Credentials): Token = {
    //I assume credentials can't be null, so I omit: if (user == null ||...
    if (credentials.username.isBlank) throw new BadContentException("Missing user ID: " + credentials.username)
    if (credentials.password.isBlank) throw new BadContentException("Missing password: " + credentials.password)
    val userId = credentials.username
    //todo should use key shared with quServer to create token
    this.synchronized {
      val user = usersByUsername.get(userId).orElse(throw new WrongCredentialsException("No such a user: " + userId))
      if (!credentials.password.equals(user.get.password)) throw new WrongCredentialsException("Wrong credentials for user: " + userId)
      //assigning "client" role to all clients
      val encryptedUserId = Jwts.builder.setSubject(userId).signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
      new Token(encryptedUserId, Role.CLIENT)
    }
  }
}
