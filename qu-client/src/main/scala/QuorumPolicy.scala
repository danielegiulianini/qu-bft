import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import qu.protocol.{ConcreteQuModel, JacksonMarshallerFactory, MarshallerFactory, MethodDescriptorFactory}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

import qu.protocol.ConcreteQuModel._


trait QuorumPolicy[U] {
  def quorum[T](operation: Operation[T, U], ohs: OHS[U]): Future[(T, Int, OHS[U])]
}

trait SimpleBroadcastPolicy[U, MarshallableA[_]] extends QuorumPolicy[U] {
  implicit val scheduler: SchedulerService = Scheduler.singleThread("") //concurrency level configurable by user??

  //is it possible to have overlapping calls to schedule (only so it's convenient to use >1 threads)?? no, actually!

  //overridinabstract tye istead of passing type param through
  trait A extends GrpcClientStub[U] {
    override type Marshallable[A] = MarshallableA[A]
  }

  type AType = A
  private val servers: Map[ConcreteQuModel.ServerId, AType] = Map()

  //protected for not exposing quorum def to client (for emulating costructor with private field)...it's not allowed otherwise!
  override def quorum[T](operation: Operation[T, U], ohs: OHS[U]):
  Future[(T, Int, OHS[U])] = {
    //private nested method
    def quorumImpl2[T](operation: Operation[T, U], ohs: OHS[U],
                       promise: Promise[(Set[Response[T, U]], OHS[U])],
                       successSet: Map[ServerId, Response[T, U]])(implicit marshallable: MarshallableA[T],
                                                                  marshallable2: MarshallableA[U],
                                                                  marshallable3a: MarshallableA[Request[T, U]],
                                                                  marshallable4: MarshallableA[Response[T, U]]):
    Unit = {
      //var currentSuccessSet = successSet;
      //qui devo recuperare l'id del server e aggiornare la ohs attribuendo a lui...
      var myOhs: OHS[U] = ohs
      var currentSuccessSet = successSet;
      val cancelable = scheduler.scheduleOnce(3.seconds)(quorumImpl2(operation, myOhs, promise, successSet)) //passing all the servers  the first time
      (servers -- successSet.keySet)
        .map(kv => (kv._1,
          //PUNTO DOVE ESPONGO IMPLEMENTAZIONE!
          kv._2.send[T](operation = Request[T, U](operation, ohs)))) //why this (more readable) nt working??:  .map((k: ServerId, s: JacksonClientStub[U]) => (k, s.send[T](operation = Request(operation), callOptions = CallOptions.DEFAULT)(enc = a, implicitly, implicitly,implicitly)))
        .foreach(kv => kv._2.onComplete({
          case Success(response) if response.responseCode == ConcreteQuModel.StatusCode.SUCCESS =>
            //here a mutex...
            currentSuccessSet = currentSuccessSet + ((kv._1, response))
            myOhs = myOhs + (kv._1 -> response.authenticatedRh)
            if (currentSuccessSet.size == 5) { //5 is quorumSize
              cancelable.cancel()
              promise success ((successSet.values.toSet, myOhs))
            }
          case _ => //this can be removed by pecifying other...
        }))
    }

    val promise = Promise[(Set[Response[T, U]], OHS[U])]()
    for {
      (successSet, ohs) <- promise.future
      //extract response
    } yield (2, 2, ohs)
    null
  }
}