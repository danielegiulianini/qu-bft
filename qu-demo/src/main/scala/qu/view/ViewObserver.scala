package qu.view

/**
 * Represents a (Gof) observer for a [[View]].
 */
trait ViewObserver {
  def start(): Unit

  def quit(): Unit
}
