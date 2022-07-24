package qu.model

import qu.model.QuorumSystemThresholdQuModel.ServerId

/**
 * Models the possible outcomes resulting from the interaction with a [[SmrSystem]].
 */
sealed trait SmrEventResult

/**
 * Models the possible outcomes resulting from counter-operations submission to a [[SmrSystem]].
 */
sealed trait CounterEventResult extends SmrEventResult

object IncResult extends CounterEventResult

object DecResult extends CounterEventResult

object ResetResult extends CounterEventResult

case class ValueResult(value: Int) extends CounterEventResult

/**
 * Models the possible outcomes resulting from cluster-management submission to a [[SmrSystem]].
 */
sealed trait ServerEventResult extends SmrEventResult

case class ServerKilled(id: ServerId, serversStatuses: Map[ServerId, ServerStatus]) extends ServerEventResult

case class ServersProfiled(serversStatuses: Map[ServerId, ServerStatus]) extends ServerEventResult

/**
 * Models the status of a replica.
 */
trait ServerStatus

object ServerStatus {
  object ACTIVE extends ServerStatus

  object INACTIVE extends ServerStatus

  object SHUTDOWN extends ServerStatus
}
