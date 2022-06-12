package qu.model

import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.Future
import scala.util.Try

trait SyncSmrSystem {

  def killServer(sid: ServerId): Try[ServerEventResult]

  def increment(): Try[CounterEventResult]

  def decrement(): Try[CounterEventResult]

  def value(): Try[CounterEventResult]

  def reset(): Try[CounterEventResult]

  def stop(): Unit
}
