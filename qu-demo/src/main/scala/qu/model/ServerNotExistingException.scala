package qu.model


/**
 * Signals that the server you want to kill does not exist because of the id provided is not valid.
 *
 * @param message the message further describing the exception.
 */
case class ServerNotExistingException(message: String) extends Exception

object ServerNotExistingException {
  //default message
  def apply(): ServerNotExistingException = ServerNotExistingException("Server id not valid. Server not existing.")
}