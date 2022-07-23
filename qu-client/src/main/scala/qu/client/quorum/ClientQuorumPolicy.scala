package qu.client.quorum


import qu.model.{ConcreteQuModel, QuorumSystemThresholds, StatusCode}
import qu.{SocketAddress, ResponsesGatherer, Shutdownable}


import scala.concurrent.{ExecutionContext, Future}

//import that declares specific dependency
import qu.model.ConcreteQuModel._

/**
 * Strategy (GoF pattern) responsible for: quorum selection (among all the replicas) and quorum involvement,
 * so managing all the client-server interaction logic.
 * @tparam ObjectT the type of the object replicated by Q/U servers on which operations are to be submitted.
 * @tparam Transportable the higher-kinded type of the strategy responsible for protocol messages (de)serialization.
 */
trait ClientQuorumPolicy[ObjectT, Transportable[_]] extends Shutdownable {
  def quorum[AnswerT](operation: Option[Operation[AnswerT, ObjectT]],
                      ohs: OHS)
                     (implicit
                      transportableRequest: Transportable[Request[AnswerT, ObjectT]],
                      transportableResponse: Transportable[Response[Option[AnswerT]]])
  : Future[(Option[AnswerT], Int, OHS)]
}


object ClientQuorumPolicy {
  //to be referenced in code when passing its instances as Higher-Order parameter
  type ClientQuorumPolicyFactory[ObjectT, Transportable[_]] =
    (Set[SocketAddress], QuorumSystemThresholds) => ClientQuorumPolicy[ObjectT, Transportable]
}
