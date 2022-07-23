package qu.model



/**
 * Signals that a server previously killed can't be killed twice.
 *
 * @param message the message further describing the exception.
 */
case class ServerAlreadyKilledException(message: String) extends Exception

object ServerAlreadyKilledException {
  //default message
  def apply(): ServerAlreadyKilledException = ServerAlreadyKilledException("Server already previously killed, can't kill it twice.")
}