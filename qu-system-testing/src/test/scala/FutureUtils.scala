import scala.concurrent.{ExecutionContext, Future}

object FutureUtils {

  //adapted from https://stackoverflow.com/questions/20414500/how-to-do-sequential-execution-of-futures-in-scala
  def seqFutures[T, U](items: IterableOnce[T])(fun: T => Future[U])(implicit ec:ExecutionContext): Future[List[U]] = {
    items.iterator.foldLeft(Future.successful[List[U]](Nil)) {
      (f, item) =>
        f.flatMap {
          x => fun(item).map(_ :: x)
        }
    } map (_.reverse)
  }

}
