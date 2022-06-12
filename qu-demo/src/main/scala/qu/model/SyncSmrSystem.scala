package qu.model

import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.Future
import scala.util.Try

trait SyncSmrSystem {

  //@throws(classOf[qu.model.ThresholdsExceededExeption])
  def killServer(sid: ServerId): Try[ServerEventResult]

  def increment(): CounterEventResult

  //def decrement(): Unit

  def value(): CounterEventResult

  def reset(): CounterEventResult

  def stop(): Unit
}
