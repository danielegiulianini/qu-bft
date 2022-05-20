//class not dependent on specific transport layer
package qu.auth

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
      usersByUsername + (user.username, toBeAdded)
    }
  }

  @throws[BadContentException]
  @throws[WrongCredentialsException]
  def authorize(credentials: Credentials): Nothing = {
    //I assume credentials can't be null, so I omit: if (user == null ||...
    if (credentials.username.isBlank) throw new BadContentException("Missing user ID: " + credentials.username)
    if (credentials.password.isBlank) throw new BadContentException("Missing password: " + credentials.password)
    val userId = credentials.username
    this.synchronized {
      val user = usersByUsername.get(userId).orElse(throw new WrongCredentialsException("No such a user: " + userId))
      if (!credentials.password.equals(user.get)) throw new WrongCredentialsException("Wrong credentials for user: " + userId)
      new Token(user.get.username, Role.CLIENT)
    }
  }
}
