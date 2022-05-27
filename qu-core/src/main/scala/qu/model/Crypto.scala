package qu.model

import javax.crypto.{KeyGenerator, Mac, SecretKey}


trait CryptoMd5Authenticator {
  self: QuModel with AbstractAbstractQuModel => //needs the ordering defined by SortedSet

  override type HMAC = String //so authenticator is a map[ServerId, String]

  override def nullAuthenticator: α = Map[String, String]()

  //adapted from https://gist.github.com/ohac/310945/7642d5432ca5f38d6341d7e7076073d98354c1a7, leveraging sortedSet ordering here
  def hmac(key: String, replicaHistory: ReplicaHistory): HMAC = {
    hmacString(key, replicaHistory.hashCode().toString)
  }

  def hmacString(key:String, data:String): String ={
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    val sha256_HMAC = Mac.getInstance("HmacSHA256")
    val secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256")
    sha256_HMAC.init(secret_key)

    sha256_HMAC.doFinal(data.getBytes("UTF-8")).map(_.toString).mkString(",")
  }


  def updateAuthenticatorFor(keys: Map[ServerId, String])(serverIdToUpdate: ServerId)(replicaHistory: ReplicaHistory): α
  = if (replicaHistory == emptyRh) nullAuthenticator //could be removed as updatedReplicaHistory will not ever hadve
  else fillAuthenticatorFor(keys)(serverIdToUpdate)(hmac(_, replicaHistory))

}


