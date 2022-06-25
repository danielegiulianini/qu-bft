package qu.auth.common

import io.grpc.Status

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//less idiomatic than try

/**
 * Contains utilities for mapping failing [[scala.concurrent.Future]] unlike [[scala.concurrent.Future.map]]
 * which applies its mapping function only to the succeeded future.
 */
object FutureUtilities {
  //grpc-specific
  def mapThrowableByStatus[T](f: Future[T], f2: Throwable => Status)(implicit executor: ExecutionContext): Future[T] =
    mapThrowable(f, error => {
      f2(error).withDescription(error.getMessage()).withCause(error).asException()
    })


  /**
   * Maps a failing future containing a Throwable to a Future containing a Throwable according to
   * the mapping provided, keeping it as it is if it's not failing.
   * @param f future to map.
   * @param exFact mapping to be applied to the Throwable contained in the failed future to generate the
   *               content of the new future.
   * @param executor the execution context running the future.
   * @tparam T the result type of the future to map.
   * @return the future containing, if failed, the Throwable resulting from the provided mapping.
   */
  def mapThrowable[T](f: Future[T], exFact: Throwable => Throwable)(implicit executor: ExecutionContext): Future[T] =
  //reusable, general utility that could be plugged with implicit conversion on Future type
    f transform {
      case s@Success(_) => s
      case Failure(cause) => Failure(exFact(cause))
    }
}
