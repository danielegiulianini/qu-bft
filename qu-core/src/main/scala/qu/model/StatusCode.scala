package qu.model

sealed trait StatusCode

object StatusCode {
  case object SUCCESS extends StatusCode

  case object FAIL extends StatusCode
}
