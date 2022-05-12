package qu.protocol.model


//could be a class separated from QUModel, bound (for LogicalTimestamp) to a ConcreteImplementation
trait Storage {
  self: AbstractQuModel =>

  def store[T, U](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])): Unit

  def retrieve[T, U](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])]

}

trait InMemoryStorage extends Storage {
  self: AbstractQuModel =>

  override def store[T, U](logicalTimestamp: LogicalTimestamp, objectAndAnswer: (U, Option[T])) = ???

  override def retrieve[T, U](logicalTimestamp: LogicalTimestamp): Option[(U, Option[T])] = ???

}

trait PersistentStorage extends Storage {
  self: AbstractQuModel =>

}

trait PersistentCachingStorage extends Storage {
  self: AbstractQuModel =>

}


