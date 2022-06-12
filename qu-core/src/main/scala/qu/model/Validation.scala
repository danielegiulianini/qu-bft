package qu.model

object Validation {
  def requireNotNullAsInvalid[T](obj: T, msg: String):Unit = if (obj == null) throw new IllegalArgumentException(msg)
  def requireNotNullAsInvalid[T](obj: T) :Unit = if (obj == null) throw new IllegalArgumentException
}
