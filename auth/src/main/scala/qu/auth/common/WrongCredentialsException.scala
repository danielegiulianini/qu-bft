package qu.auth.common


/**
 * Signals that user-provided credentials are not the ones provided at registration time.
 *
 * @param message the message further describing the exception.
 */
case class WrongCredentialsException(message: String) extends Exception