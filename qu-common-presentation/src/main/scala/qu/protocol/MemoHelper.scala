package qu.protocol

//memo helper def not as inner def since it could be reused elsewhere
object MemoHelper {
  def memoize[I, O](f: I => O): I => O = new collection.mutable.HashMap[I, O]() {
    override def apply(key: I) = getOrElseUpdate(key, f(key))
  }
}
