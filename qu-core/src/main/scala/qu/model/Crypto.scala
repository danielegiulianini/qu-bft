package qu.model


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


