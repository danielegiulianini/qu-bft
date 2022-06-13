package qu


trait AbstractRecipientInfo {
  def ip: String

  def port: Int
}

case class RecipientInfo(ip: String, port: Int) extends AbstractRecipientInfo

object RecipientInfo {
  //could be a method of RecipientInfo
  def id(serverInfo: AbstractRecipientInfo): String = serverInfo.ip + ":" + serverInfo.port
}