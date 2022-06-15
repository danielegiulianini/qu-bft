package qu.model

case class ServerAlreadyKilledException(message: String) extends Exception

object ServerAlreadyKilledException {
  //default message
  def apply(): ServerAlreadyKilledException = ServerAlreadyKilledException("Server already previously killed, can't kill it twice.")
}