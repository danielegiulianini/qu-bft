package qu.model

trait Hashing {
  self: QuModel with AbstractAbstractQuModel =>

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

   def hashObject(obj: Any): String =
    //obj.hashCode().toString //obj.toString().md5.hex   //since obj is never null Objects.hashcode not required...(not to use in distributed applications
  {
    import java.nio.charset.StandardCharsets
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(obj.toString.getBytes(StandardCharsets.UTF_8))
    hash.map(_.toString).mkString(",")
  }

}
