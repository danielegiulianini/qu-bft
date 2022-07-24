package qu.auth.common

/**
 * Signals that user-provided content does not respect an agreed protocol.
 *
 * @param message the message further describing the exception.
 */
case class BadContentException(message: String) extends Exception
