package qu.model

/**
 * Defines the status codes modelling some of expected outcomes of protocol interactions.
 */
object StatusCode extends Enumeration {
  type StatusCode = Value
  val SUCCESS, FAIL = Value
}