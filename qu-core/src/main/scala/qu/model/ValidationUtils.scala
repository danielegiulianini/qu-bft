package qu.model

/**
 * Some utilities for the validation of an object reference.
 */
object ValidationUtils {
  def requireNonNullAsInvalid[T](obj: T, msg: String):Unit = if (obj == null) throw new IllegalArgumentException(msg)
  def requireNonNullAsInvalid[T](obj: T) :Unit = if (obj == null) throw new IllegalArgumentException
}
