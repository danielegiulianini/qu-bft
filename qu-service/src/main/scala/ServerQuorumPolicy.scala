import Shared.{QuorumSystemThresholds, RecipientInfo}
import com.fasterxml.jackson.module.scala.JavaTypeable

import scala.concurrent.Future

trait ServerQuorumPolicy[Marshallable[_], U] {
  def objectSync[T](): Future[Option[(U, T)]]
}

class SimpleServerQuorumPolicy[U, Marshallable[_]](stubs: Map[String, GrpcClientStub[Marshallable]])
  extends ServerQuorumPolicy[Marshallable, U] {
  override def objectSync[T](): Future[Option[(U, T)]] = null
}

object ServerQuorumPolicy{
  type ServerQuorumPolicyFactory[Marshallable[_], U] = (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Marshallable, U]

  def simpleJacksonServerQuorumFactory[U]() : ServerQuorumPolicyFactory[JavaTypeable, U] = ???

}
