package qu.client.datastructures

trait ResettableCounter extends Counter with Resettable
trait Resettable {
  def reset(): Unit
}