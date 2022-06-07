package qu.model

trait OperationTypes { self:QuModel =>

  sealed trait OperationType1

  object OperationType1 {
    case object METHOD extends OperationType1

    case object INLINE_METHOD extends OperationType1

    case object COPY extends OperationType1

    case object BARRIER extends OperationType1

    case object INLINE_BARRIER extends OperationType1
  }

  override type OperationType = OperationType1
}
