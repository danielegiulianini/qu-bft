package qu.model

import qu.model.QuorumSystemThresholdQuModel.ServerId

import scala.concurrent.Future
import scala.util.Try

/**
 * A simple State Machine Replication system providing APIs for interacting with a distributed, fault-tolerant
 * counter as long as cluster management operations.
 */
trait SmrSystem {

  def killServer(sid: ServerId): Try[ServerEventResult]

  def getServersStatus: Try[ServerEventResult]

  def increment(): Try[CounterEventResult]

  def decrement(): Try[CounterEventResult]

  def value(): Try[CounterEventResult]

  def reset(): Try[CounterEventResult]

  def stop(): Unit
}
