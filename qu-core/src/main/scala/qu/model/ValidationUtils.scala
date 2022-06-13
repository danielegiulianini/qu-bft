package qu.model

object ValidationUtils {
  def requireNotNullAsInvalid[T](obj: T, msg: String):Unit = if (obj == null) throw new IllegalArgumentException(msg)
  def requireNonNullAsInvalid[T](obj: T) :Unit = if (obj == null) throw new IllegalArgumentException
}
