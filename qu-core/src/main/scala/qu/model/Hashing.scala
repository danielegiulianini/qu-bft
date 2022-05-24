package qu.model

trait Hashing {
  self: QuModel with AbstractAbstractQuModel =>

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

  //since obj is never null Objects.hashcode not required...
  private def hashObject(obj: Any) = obj.hashCode().toString //obj.toString().md5.hex

}
