package qu.model

/**
 * A module containing hashing utilities for memory-efficient timestamp storage (includes "compact timestamp"
 * optimization, as to Q/U main paper terminology.
 */
trait Hashing {

  self: QuModel with LessAbstractQuModel =>

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

   def hashObject(obj: Any): String = {
    import java.nio.charset.StandardCharsets
    import java.security.MessageDigest
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(obj.toString.getBytes(StandardCharsets.UTF_8))  //not using Objects.hashcode as it's JVM-dependent and so not recommended for distributed applications
     hash.map(_.toString).mkString("")
  }

}
