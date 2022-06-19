package qu

import monix.execution.schedulers.SchedulerService
import monix.execution.{Cancelable, Scheduler}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}

//a class exposing a promise-based and a callback-based API (monix scheduler has callback only)
class OneShotAsyncScheduler(poolSize: Int) extends Shutdownable {
  private implicit val scheduler: SchedulerService = Scheduler.fixedPool("schedulerService", poolSize)

  def scheduleOnceAsPromise(duration: FiniteDuration): Future[Unit] = {
    val promise = Promise[Unit]()
    scheduler.scheduleOnce(duration)(promise success()) //scheduler.scheduleOnce(duration)(())
    promise.future
  }

  def scheduleOnceAsCallback(duration: FiniteDuration)(action: => Unit): Cancelable = {
    scheduler.scheduleOnce(duration)(action)
  }

  override def shutdown(): Future[Unit] = Future {
    scheduler.shutdown(); scheduler.awaitTermination(5.seconds)
  }

  override def isShutdown: Boolean = scheduler.isShutdown
}

