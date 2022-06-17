package qu

object ListUtils {
  def getMostFrequentElement[T](iterable: Iterable[T]): Option[T] =
    getMostFrequentElementWithOccurrences(iterable).map(_._1)

  def getMostFrequentElementWithOccurrences[T](iterable: Iterable[T]): Option[(T, Int)] =
    occurrences(iterable).maxByOption(_._2)

  def occurrences[A](as: Iterable[A]): Map[A, Int] =
    as.groupMapReduce(identity)(_ => 1)(_ + _)
}
