import monix.execution.{Cancelable, Scheduler}
import monix.execution.schedulers.SchedulerService

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Future, Promise}

//a class exposing a promise-based and a callback-based API (monix scheduler has callback only)
//scheduler.scheduleOnce(3.seconds)(/*callback based API*/)
class MyScheduler(poolSize: Int) {
  private implicit val scheduler: SchedulerService = Scheduler.fixedPool("schedulerService", poolSize)

  def scheduleOnceAsPromise(duration: FiniteDuration): Future[Void] = {
    val promise = Promise()
    scheduler.scheduleOnce(duration)(())
    promise.future
  }

  def scheduleOnceAsCallback(duration: FiniteDuration)(action: => Unit): Cancelable = {
    scheduler.scheduleOnce(duration)(action)
  }
}

