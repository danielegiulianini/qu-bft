package qu.model

case class ServerNotExistingException(message: String) extends Exception

object ServerNotExistingException {
  //default message
  def apply(): ServerNotExistingException = ServerNotExistingException("Server id not valid. Server not existing.")
}