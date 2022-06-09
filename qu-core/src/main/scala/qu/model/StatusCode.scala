package qu.model

/*sealed trait StatusCode
object StatusCode {
  case object SUCCESS extends StatusCode

  case object FAIL extends StatusCode
}*/

object StatusCode extends Enumeration {
  type StatusCode = Value
  val SUCCESS, FAIL = Value
}