package qu.model

import qu.model.ConcreteQuModel.{Operation, ServerId}
import qu.model.examples.Commands.Increment


sealed trait SmrEventResult


sealed trait CounterEventResult extends SmrEventResult {
  def of: Operation[_, Int]
}

object IncResult extends CounterEventResult {
  def of = Increment()
}

object DecResult extends CounterEventResult {
  def of: Increment = ???


}

object ResetResult extends CounterEventResult {
  def of: Increment = ???
}

case class ValueResult(value: Int) extends CounterEventResult {
  def of: Increment = ???
}

sealed trait ServerEventResult extends SmrEventResult

case class ServerKilled(id: ServerId, serversStatuses: Map[ServerId, ServerStatus]) extends ServerEventResult

trait ServerStatus

object ServerStatus {
  object ACTIVE extends ServerStatus

  object INACTIVE extends ServerStatus

  object SHUTDOWN extends ServerStatus
}
