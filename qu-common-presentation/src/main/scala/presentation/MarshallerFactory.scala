package presentation

import io.grpc.MethodDescriptor

/**
 * A (GoF) factory method for [[io.grpc.MethodDescriptor.Marshaller]]s.
 * @tparam Marshallable the higher-kinded type of the strategy responsible for messages (de)serialization.
 */
trait MarshallerFactory[Marshallable[_]] {
  def marshallerFor[T: Marshallable]: MethodDescriptor.Marshaller[T]
}
