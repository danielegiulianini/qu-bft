import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import qu.protocol.{ConcreteQuModel, MarshallerFactory, Messages, MethodDescriptorFactory}
import qu.protocol.ConcreteQuModel.{OHS, ServerId, emptyOhs}
import qu.protocol.Messages.{Request, Response}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

trait QuorumPolicy[U] {
  def quorum[T](servers: Map[ServerId, GrpcClientStub[U]],
                operation: Messages.Operation[T, U]): Future[(T, Int, OHS[U])]
}

//a trait to be mixed or a concrete class to compose QuClientImpl???
abstract class SimpleBroadcastPolicy[U] extends QuorumPolicy[U] {
  self: GrpcClientStub[U]=>
  //self needed for marshaller type definition...

  var ohs: OHS[U] = emptyOhs
  val servers: Map[ConcreteQuModel.ServerId, GrpcClientStub[U]] = Map[ServerId, GrpcClientStub[U]]()

  //"A thread-pool with an exact number of threads (and not a variable one like the ForkJoinPool above), backed
  // by a Java ScheduledThreadPool for both executing and scheduling delays"
  implicit val scheduler: SchedulerService = Scheduler.singleThread("") //concurrency level configurable by user??
  //is it possible to have overlapping calls to schedule (only so it's convenient to use >1 threads)?? no, actually!

  //protected for not exposing quorum def to client...it's not allowed!
  override def quorum[T](servers: Map[ServerId, GrpcClientStub[U]],
                         operation: Messages.Operation[T, U]):
  Future[(T, Int, OHS[U])] = {
    val promise = Promise[(Set[Response[T, U]], OHS[U])]()
    for {
      (successSet, ohs) <- promise.future
      //extract response
    } yield (2, 2, ohs)
    null
  }

  def quorumImpl2[T](operation: Messages.Operation[T, U],
                     promise: Promise[(Set[Response[T, U]], OHS[U])],
                     successSet: Map[ServerId, Response[T, U]])(implicit marshallable: Marshallable[T]):
  Unit = { //or  return the promise itlsef, but would be redundant
    /*var currentSuccessSet = successSet;
    val SUCCESS = 0 //todo be replaced by enum
    val cancelable = scheduler.scheduleOnce(3.seconds)(quorumImpl2(operation, promise, successSet)) //passing all the servers  the first time
    (servers -- successSet.keySet)
      .map(kv => (kv._1,
        kv._2.send[T](operation = Request(operation)))) //why this (more readable) nt working??:  .map((k: ServerId, s: JacksonClientStub[U]) => (k, s.send[T](operation = Request(operation), callOptions = CallOptions.DEFAULT)(enc = a, implicitly, implicitly,implicitly)))
      .foreach(kv => kv._2.onComplete({
        case Success(response) if response.responseCode == SUCCESS =>
          //here a mutex...
          currentSuccessSet = currentSuccessSet + ((kv._1, response))
          ohs = ohs + (kv._1 -> response.authenticatedRh)
          if (currentSuccessSet.size == 5) { //5 is quorumSize
            cancelable.cancel()
            promise success ((successSet.values.toSet, ohs))
          }
        case _ => //this can be removed by pecifying other...
      }))*/
  }
}

/* INTERESTING, CLOSER TO PAPER DEFINITION:

  //utility method that passes the promise through...
  def quorumImpl[T](stillNotAnswered: Map[ServerId, GrpcClientStub[U]],
                    operation: Messages.Operation[T, U],
                    promise: Promise[(Set[Response[T, U]], OHS[U])],
                    successSet: Set[Response[T, U]])
                   (implicit a: Marshallable[T],
                    b: Marshallable[U]):
  Unit = { //or  return the promise itlsef, but would be redundant
    val SUCCESS = 0 //todo be replaced by enum
    var toBeAskedThen = stillNotAnswered
    var successSet2 = successSet
    val cancelable = scheduler.scheduleOnce(3.seconds)(quorumImpl(toBeAskedThen, operation, promise, successSet)) //passing all the servers  the first time
    stillNotAnswered
      .map(kv => (kv._1, kv._2.send[T](operation = Request(operation))(enc = a, implicitly, implicitly, implicitly))) //why this (more readable) nt working??:  .map((k: ServerId, s: JacksonClientStub[U]) => (k, s.send[T](operation = Request(operation), callOptions = CallOptions.DEFAULT)(enc = a, implicitly, implicitly,implicitly)))
      .foreach(kv => kv._2.onComplete({
        case Success(response) if (response.responseCode == SUCCESS) =>
          successSet2 = successSet2 + response
          ohs = ohs + (kv._1 -> response.authenticatedRh) //also answer to be added
          toBeAskedThen = toBeAskedThen - kv._1
          if (ohs.size - toBeAskedThen.size == 5) { //5 is quorumSize
            cancelable.cancel()
            promise success ((successSet2, ohs))
          }
        case _ => //case Failure(_) => //this can be removed by pecifying other...
      }))
  }
 */



//there are "quorumPolicy"s that actually require obj pref quorum is defined
//or better: all requires a pref quorum, but if they ignore it, sit can be
//made of by all the servers
/*trait WithPreferredQuorum[A]
class QuorumPolicy2[T:WithPreferredQuorum, U] extends QuorumPolicy[U] {
  override protected def quorum[T](servers: Servers):
  (Response[T], Int, Map[ConcreteQuModel.ServerId, (SortedSet[(ConcreteQuModel.MyLogicalTimestamp[_, U], ConcreteQuModel.MyLogicalTimestamp[_, U])], ConcreteQuModel.α)]) =
    ???
}*/
