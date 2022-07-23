package qu.model

trait Hashing {

  self: QuModel with LessAbstractQuModel =>

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

  //not using Objects.hashcode as it's JVM-dependent and so not recommended for distributed applications
   def hashObject(obj: Any): String = {
    import java.nio.charset.StandardCharsets
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(obj.toString.getBytes(StandardCharsets.UTF_8))
    hash.map(_.toString).mkString("")
  }

}
