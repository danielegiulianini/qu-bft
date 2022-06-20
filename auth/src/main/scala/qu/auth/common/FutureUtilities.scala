package qu.auth.common

import io.grpc.Status

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

//less idiomatic than try
object FutureUtilities {
  //grpc-specific
  def mapThrowableByStatus[T](f: Future[T], f2: Throwable => Status)(implicit executor: ExecutionContext): Future[T] =
    mapThrowable(f, error => {
      f2(error).withDescription(error.getMessage()).withCause(error).asException()
    })

  //reusable, general utilities: could be plugged with implicit conversion on Future type
  def mapThrowable[T](f: Future[T], exFact: Throwable => Throwable)(implicit executor: ExecutionContext): Future[T] =
    f transform {
      case s@Success(_) => s
      case Failure(cause) => Failure(exFact(cause))
    }

  /* def mapException[T](f: Future[T], ex: Exception)(implicit executor: ExecutionContext): Future[T] =
     f transform {
     case s@Success(_) => s
     case Failure(cause) => {
       Failure(ex)
     }
   }*/
}
