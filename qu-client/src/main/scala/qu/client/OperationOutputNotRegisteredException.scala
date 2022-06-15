package qu.client

case class OperationOutputNotRegisteredException(message: String) extends Exception

object OperationOutputNotRegisteredException {
  
  //default message
  def apply(): OperationOutputNotRegisteredException =
    OperationOutputNotRegisteredException("Custom operation not registered at server side before sending it.")
}
