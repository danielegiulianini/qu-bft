import Shared.{QuorumSystemThresholds, RecipientInfo}
import com.fasterxml.jackson.module.scala.JavaTypeable

import scala.concurrent.Future

trait ServerQuorumPolicy[Marshallable[_], U] {
  def objectSync[T](): Future[Option[(U, T)]]
}

class SimpleServerQuorumPolicy[Marshallable[_], U](stubs: Map[String, GrpcClientStub[Marshallable]])
  extends ServerQuorumPolicy[Marshallable, U] {
  override def objectSync[T](): Future[Option[(U, T)]] = null
}

object ServerQuorumPolicy{
  type ServerQuorumPolicyFactory[Marshallable[_], U] = (Set[RecipientInfo], QuorumSystemThresholds) => ServerQuorumPolicy[Marshallable, U]

  //todo: this is only a stub
  def simpleJacksonServerQuorumFactory[U]() : ServerQuorumPolicyFactory[JavaTypeable, U] =
    (mySet, thresholds) => new SimpleServerQuorumPolicy[JavaTypeable, U](stubs = Map())

}
