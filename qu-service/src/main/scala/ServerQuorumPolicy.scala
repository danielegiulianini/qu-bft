import scala.concurrent.Future

trait ServerQuorumPolicy[U] {
  def objectSync[T](): Future[Option[(T, U)]]
}

class SimpleServerQuorumPolicy[U, Marshallable[_]](stubs: Map[String, GrpcClientStub[Marshallable]])
  extends ServerQuorumPolicy[U] {
  override def objectSync[T](): Future[Option[(T, U)]] = null
}
