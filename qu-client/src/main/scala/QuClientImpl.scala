//import that declares specific dependency

import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import qu.protocol.ConcreteQuModel.{classify, _}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}

class QuClientImpl[U](private var policy: QuorumPolicy[U], private var ohs: OHS) extends QuClient[U] {

  //values to inject
  val q = 2
  val r = 3

  import scala.concurrent.ExecutionContext.Implicits.global

  override def submit[T](op: Operation[T, U]): Future[T] = {
    for {
      (answer, order, ohs) <- policy.quorum(Some(op), ohs)
      answer <- if (order < q) for {
        _ <- repair(ohs) //this updates ohs
        newAnswer <- submit(op)
      } yield newAnswer else Future(answer.getOrElse(throw new RuntimeException("illegal protocol State exception..."))) //when using option: Future(answer.get)
    } yield answer
  }

  def repair(ohs: OHS): Future[OHS] = {
    def backOffAndRetry(operationType: OperationType1): Future[OHS] = for {
      _ <- backOff()
      //perform a barrier or a copy
      (_, _, ohs) <- policy.quorum(Option.empty[Operation[Object, U]], ohs) //here Object is fundamental as server could return other than T
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType)
    } yield ohs

    def classifyAsync(ohs: OHS) = Future {
      classify(ohs, r, q)
    }

    def backOffAndRetryUntilMethod(operationType: OperationType1) =
      if (operationType != OperationType1.METHOD) backOffAndRetry(operationType) else Future {
        ohs
      }

    //actual logic
    for {
      (operationType, _, _) <- classifyAsync(ohs)
      ohs <- backOffAndRetryUntilMethod(operationType)
    } yield ohs
  }

  private implicit val scheduler: SchedulerService = Scheduler.singleThread("") //concurrency level configurable by user??

  //to move in scheduler class
  private def scheduleOnceWithPromise(duration: FiniteDuration): Future[Void] = {
    val promise = Promise()
    scheduler.scheduleOnce(3.seconds)(())
    promise.future
  }

  //doing nothing
  private def backOff() = scheduleOnceWithPromise(3.seconds)
}


//companion object: consider putting utilities in QuClient if they are reusable at a higher level
object QuClientImpl {
  //type ServerRefs[U] = Map[ServerId, JacksonClientStub[U]]
  //val io = new QuClientImpl[Int] with SimpleBroadcastPolicy[Int]

  //grpc-aware factory:
  def apply[U](quorumThreshold: Int,
               repairableThreshold: Int,
               ips: Set[String] //serverRefs: ServerRefs[U] //here i need IPs

               //grpc's callOptions , key management??  (to pass to clientStub...)
              ): QuClientImpl[U] = {
    //istanzio i servers (client stub) da passare alla quorumPolicy

    null
  }

  /*def apply[U](quorumThreshold: Int,
               repairableThreshold: Int,
               ipsWithKeyPaths: Set[(String, String)]
               //serverRefs: ServerRefs[U] //here i need IPs
               //grpc's callOptions , key management??  (to pass to clientStub...)
              ): QuClientImpl[U] = {
    null
  }*/
  //una factory di QUClient... istanziando lo specifico
  //un oggetto per creare il quClient
}

object ProvaUserSide {

  //QuClientImpl(cluster)

  //QuClientImpl(cluster, thresholds)

  //QuClientImpl(thresholds,
  //ips = Set(("www.google.com", port = 2), ("www.amazon.com", port=80))
  QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ips = Set("www.google.com", "www.amazon.com"))

  /*QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ipsWithKeyPaths = Set(("www.google.com", "pathForGoogle"), ("www.amazon.com", "pathForAmazon")))*/
}