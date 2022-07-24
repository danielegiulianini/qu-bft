package qu.client.datastructures

trait Counter {
  /** Current value of this counter. */
  def value(): Int

  /** Increment this counter. */
  def increment(): Unit

  /** Decrement this counter. */
  protected def decrement(): Unit
}
