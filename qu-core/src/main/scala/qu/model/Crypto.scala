package qu.model


/**
 * A module containing cryptographic utilities used for [[ReplicaHistory]] validation.
 */
trait CryptoMd5Authenticator {
  self: QuModel with LessAbstractQuModel => //needs the ordering defined by SortedSet

  override type hMac = String

  override def nullAuthenticator: authenticator = Map[String, String]()

  //adapted from https://gist.github.com/ohac/310945/7642d5432ca5f38d6341d7e7076073d98354c1a7, leveraging sortedSet ordering here
  def hmac(key: Key, replicaHistory: ReplicaHistory): hMac = {
    hmacString(key, replicaHistory.hashCode().toString)
  }

  def hmacString(key: String, data: String): String = {
    import javax.crypto.Mac
    import javax.crypto.spec.SecretKeySpec
    val sha256_HMAC = Mac.getInstance("HmacSHA256")
    val secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256")
    sha256_HMAC.init(secret_key)

    sha256_HMAC.doFinal(data.getBytes("UTF-8")).map(_.toString).mkString("")
  }

  def authenticateRh(rh: ReplicaHistory, keys: Map[ServerId, Key]): authenticator =
    keys.view.mapValues(hmac(_, rh)).toMap

}


