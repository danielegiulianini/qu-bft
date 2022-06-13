package qu

object ListUtils {
  def getMostFrequentElement[T](iterable: Iterable[T]): Option[T] =
    iterable.groupMapReduce(identity)(_ => 1)(_ + _).maxByOption(_._2).map(_._1)
}
