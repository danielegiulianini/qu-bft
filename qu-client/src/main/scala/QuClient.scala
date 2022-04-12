import scala.concurrent.Future

//most abstract possible (not bound to grpc)
trait QuClient {
  def submit[T, U](op: qu.protocol.Messages.Operation[T, U]): Future[T]
}
