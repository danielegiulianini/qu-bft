package qu.auth.common

/**
 * Signals that a resource claimed by the user is already assigned.
 * @param message the message further describing the exception.
 */
case class ConflictException(message:String) extends Exception
