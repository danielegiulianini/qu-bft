import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._


trait QuorumPolicy[U, Marshallable[_]] {
  def quorum[T](operation: Option[Operation[T, U]],
                ohs: OHS)
               (implicit
                marshallableRequest: Marshallable[Request[T, U]],
                marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Option[T], Int, OHS)]
}


//basic policy
trait SimpleBroadcastPolicy[U, Marshallable[_]] extends QuorumPolicy[U, Marshallable] {

  //vakues to inject
  val q = 2
  val r = 3

  //concurrency level configurable by user??
  //is it possible to have overlapping calls to schedule
  // (only so it's convenient to use >1 threads)?? no, actually!
  //dependencies shared with QuClient
  val scheduler = new OneShotAsyncScheduler(2)
  protected val servers: Map[ServerId, GrpcClientStub[Marshallable]]

  override def quorum[T](operation: Option[Operation[T, U]],
                         ohs: OHS)
                        (implicit
                         marshallableRequest: Marshallable[Request[T, U]],
                         marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Option[T], Int, OHS)] = {
    //private nested method
    def quorumImpl2(completionPromise: Promise[(Set[Response[Option[T]]], OHS)],
                    successSet: Map[ServerId, Response[Option[T]]])
                   (implicit
                    marshallableRequest: Marshallable[Request[T, U]],
                    marshallableResponse: Marshallable[Response[Option[T]]]): Future[(Set[Response[Option[T]]], OHS)] = {
      var myOhs: OHS = ohs
      var currentSuccessSet = successSet
      val cancelable = scheduler.scheduleOnceAsCallback(3.seconds)(quorumImpl2(completionPromise, successSet)) //passing all the servers  the first time

      (servers -- successSet.keySet)
        .map(kv => (kv._1,
          kv._2.send2[Request[T, U], Response[Option[T]]](toBeSent = Request(operation, ohs))))
        .foreach(kv => kv._2.onComplete({
          case Success(response) if response.responseCode == StatusCode.SUCCESS =>
            //mutex needed because of multithreaded ex context
            this.synchronized {
              myOhs = myOhs + (kv._1 -> response.authenticatedRh)
              currentSuccessSet = currentSuccessSet + ((kv._1, response))
              if (currentSuccessSet.size == q) {
                cancelable.cancel()
                completionPromise success ((successSet.values.toSet, myOhs))
              }
            }
          case _ => //no need to do nothing, only waiting for other servers' responses
        }))

      completionPromise.future
    }

    def getElected(responses: Set[Response[Option[T]]]) = Future {
      responses
        .groupMapReduce(identity)(_ => 1)(_ + _)
        .maxByOption(_._2)
        .getOrElse(throw new RuntimeException("inconsistent protocl state"))
    }

    //actual logic
    for {
      (responses, ohs) <- quorumImpl2(Promise(), successSet = Map())
      (response, voteCount) <- getElected(responses)
    } yield (response.answer, voteCount, ohs)
  }

}