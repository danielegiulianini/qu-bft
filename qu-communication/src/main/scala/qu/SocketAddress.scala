package qu


trait AbstractSocketAddress {
  def ip: String

  def port: Int
}

case class SocketAddress(ip: String, port: Int) extends AbstractSocketAddress

object SocketAddress {

  def id(abstractSocketAddress: AbstractSocketAddress): String =
    abstractSocketAddress.ip + ":" + abstractSocketAddress.port
}