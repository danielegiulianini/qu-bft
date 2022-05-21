package qu.protocol.model


trait CryptoMd5Authenticator {
  self: QuModel with AbstractAbstractQuModel => //needs the ordering defined by SortedSet

  override type HMAC = String //so authenticator is a map[ServerId, String]

  override val nullAuthenticator: α = Map[String, String]()

  import com.roundeights.hasher.Implicits._

  //leveraging sortedSet ordering here
  def hmac(key: String, replicaHistory: ReplicaHistory): HMAC = {
    replicaHistory.hashCode().toString().hmac(key).md5
  }

  def updateAuthenticatorFor(keys: Map[ServerId, String])(serverIdToUpdate: ServerId)(replicaHistory: ReplicaHistory): α
  = if (replicaHistory == emptyRh) nullAuthenticator //could be removed as updatedReplicaHistory will not ever hadve
  else fillAuthenticatorFor(keys)(serverIdToUpdate)(hmac(_, replicaHistory))

}

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

