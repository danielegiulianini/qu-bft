import scala.concurrent.Future

//most abstract possible (not bound to grpc)
trait QuClient[U] {
  def submit[T](op: qu.protocol.Messages.Operation[T, U]): Future[T]
}
