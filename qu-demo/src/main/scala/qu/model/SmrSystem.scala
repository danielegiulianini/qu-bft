package qu.model

import qu.model.ConcreteQuModel.ServerId

import scala.concurrent.Future
import scala.util.Try

trait SmrSystem {

  def killServer(sid: ServerId): Try[ServerEventResult]

  def getServersStatus(): Try[ServerEventResult]

  def increment(): Try[CounterEventResult]

  def decrement(): Try[CounterEventResult]

  def value(): Try[CounterEventResult]

  def reset(): Try[CounterEventResult]

  def stop(): Unit
}
