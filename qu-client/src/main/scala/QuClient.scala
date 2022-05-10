import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._

//most abstract possible (not bound to grpc)
trait QuClient[U, Marshallable[_]] {
  def submit[T](op: Operation[T, U])(implicit
                                     marshallableRequest: Marshallable[Request[T, U]],
                                     marshallableResponse: Marshallable[Response[Option[T]]],
                                     marshallableRequestObj: Marshallable[Request[Object, U]],
                                     marshallableResponseObj: Marshallable[Response[Option[Object]]]): Future[T]
}
