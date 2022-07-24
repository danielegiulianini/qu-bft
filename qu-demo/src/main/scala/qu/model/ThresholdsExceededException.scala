package qu.model

/**
 * Signals that the quorum system thresholds that guarantees correct protocol semantics would be
 * broken, so breaking it too.
 *
 * @param message the message further describing the exception.
 */
case class ThresholdsExceededException(message: String) extends Exception //exceptions case classes to enable cleaner pattern matching


object ThresholdsExceededException {
  //(GoF) Factory method with default message
  def apply(): ThresholdsExceededException = ThresholdsExceededException("Quorum System thresholds, which guarantee the correct protocol " +
    "semantics, would be exceeded")
}