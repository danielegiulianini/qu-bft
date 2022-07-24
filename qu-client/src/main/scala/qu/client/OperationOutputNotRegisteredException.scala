package qu.client


/**
 * Signals that the output type of an operation submitted by a client to a service has
 * not been previously registered at service side.
 *
 * @param message the message further describing the exception.
 */
case class OperationOutputNotRegisteredException(message: String) extends Exception

object OperationOutputNotRegisteredException {
  
  //default message
  def apply(): OperationOutputNotRegisteredException =
    OperationOutputNotRegisteredException("Custom operation not registered at server side before sending it.")
}
