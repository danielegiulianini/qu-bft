package qu.model

import qu.model.ConcreteQuModel.{Operation, ServerId}
import qu.model.examples.Commands.Increment


sealed trait SmrEventResult


sealed trait CounterEventResult extends SmrEventResult

object IncResult extends CounterEventResult

object DecResult extends CounterEventResult

object ResetResult extends CounterEventResult

case class ValueResult(value: Int) extends CounterEventResult

sealed trait ServerEventResult extends SmrEventResult

case class ServerKilled(id: ServerId, serversStatuses: Map[ServerId, ServerStatus]) extends ServerEventResult

trait ServerStatus

object ServerStatus {
  object ACTIVE extends ServerStatus

  object INACTIVE extends ServerStatus

  object SHUTDOWN extends ServerStatus
}
