package qu.model

//exceptions case classes to enable cleaner pattern matching
case class ThresholdsExceededException(message: String) extends Exception

object ThresholdsExceededException {
  //default message
  def apply(): Unit = ThresholdsExceededException("Quorum System thresholds, which guarantee the correct protocol " +
    "semantics would be exceeded, so threatening it.")

}