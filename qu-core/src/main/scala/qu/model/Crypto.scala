package qu.model

import javax.crypto.{KeyGenerator, Mac}


trait CryptoMd5Authenticator {
  self: QuModel with AbstractAbstractQuModel => //needs the ordering defined by SortedSet

  override type HMAC = String //so authenticator is a map[ServerId, String]

  override val nullAuthenticator: α = Map[String, String]()



  //leveraging sortedSet ordering here
  def hmac(key: String, replicaHistory: ReplicaHistory): HMAC = {
    //import com.roundeights.hasher.Implicits._
    //replicaHistory.hashCode().toString().hmac(key).md5
    val keygen = KeyGenerator.getInstance("HmacSHA1")
    val secret = keygen.generateKey()
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(secret)
    val result: Array[Byte] = mac.doFinal("foo".getBytes)
    result.map(_.toString).mkString(",")
  }

  def updateAuthenticatorFor(keys: Map[ServerId, String])(serverIdToUpdate: ServerId)(replicaHistory: ReplicaHistory): α
  = if (replicaHistory == emptyRh) nullAuthenticator //could be removed as updatedReplicaHistory will not ever hadve
  else fillAuthenticatorFor(keys)(serverIdToUpdate)(hmac(_, replicaHistory))

}


