package qu.protocol.model


trait CryptoMd5Authenticator {
  self: AbstractAbstractQuModel => //needs the ordering defined by SortedSet

  override type HMAC = String

  override def nullHMAC(key: String) = hmac(key, emptyRh)

  import com.roundeights.hasher.Implicits._ // import com.roundeights.hasher.Digest.digest2string

  //leveraging sortedSet ordering here
  def hmac(key: String, replicaHistory: ReplicaHistory): HMAC =
  //should be taken over the hash of a replicahistory
    replicaHistory.hashCode().toString().hmac(key).md5

  def updateAuthenticatorFor(keys: Map[ServerId, String])(serverIdToUpdate: ServerId)(replicaHistory: ReplicaHistory): α =
    fillAuthenticatorFor(keys)(serverIdToUpdate)(hmac(_, replicaHistory))

  override type OperationRepresentation = String

  override def represent[T, U](operation: Option[Operation[T, U]]): OperationRepresentation =
    hashObject(operation)

  override def represent(ohs: OHS): OHSRepresentation =
    hashObject(ohs)

  //or Objects.hash ...??
  private def hashObject(obj: Any) = obj.hashCode().toString //obj.toString().md5.hex

}

