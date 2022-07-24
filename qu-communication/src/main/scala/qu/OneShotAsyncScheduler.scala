package qu

import monix.execution.schedulers.SchedulerService
import monix.execution.{Cancelable, Scheduler}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}

/**
 * An async scheduler exposing one-shot, promise-based and callback-based API (monix scheduler has callback only).
 */
class OneShotAsyncScheduler(poolSize: Int) extends Shutdownable {
  private implicit val scheduler: SchedulerService = Scheduler.fixedPool("schedulerService", poolSize)

  def scheduleOnceAsPromise(duration: FiniteDuration): Future[Unit] = {
    val promise = Promise[Unit]()
    scheduler.scheduleOnce(duration)(promise success())
    promise.future
  }

  def scheduleOnceAsCallback(duration: FiniteDuration)(action: => Unit): Cancelable = {
    scheduler.scheduleOnce(duration)(action)
  }

  override def shutdown(): Future[Unit] = Future {}

  override def isShutdown: Boolean = scheduler.isShutdown
}

