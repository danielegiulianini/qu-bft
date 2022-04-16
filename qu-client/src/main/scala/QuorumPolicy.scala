import qu.protocol.ConcreteQuModel
import qu.protocol.ConcreteQuModel.OHS
import qu.protocol.Messages.Response

import scala.collection.SortedSet

trait QuorumPolicy[U] {
  def quorum[T](servers: Servers): (Response[T], Int, OHS[U])
}

//there are "quorumPolicy"s that actually require obj pref quorum is defined
//or better: all requires a pref quorum, but if they ignore it, sit can be
//made of by all the servers
trait WithPreferredQuorum[A]
class QuorumPolicy2[T:WithPreferredQuorum] extends QuorumPolicy {
  override protected[this] def quorum[T, U](servers: Servers):
  (Response[T], Int, Map[ConcreteQuModel.ServerId, (SortedSet[(ConcreteQuModel.MyLogicalTimestamp[_, U], ConcreteQuModel.MyLogicalTimestamp[_, U])], ConcreteQuModel.α)]) =
    ???
}


