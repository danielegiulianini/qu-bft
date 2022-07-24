package qu.view.console

import qu.model.QuorumSystemThresholdQuModel.ServerId

sealed trait AbstractCliCmd {
  def command: String

  def arguments: String

  def descriptions: String
}

case class CliCmdWithoutArg(command: String,
                            arguments: String,
                            descriptions: String) extends AbstractCliCmd

case class CliCmdWithArg[T](command: String,
                            arguments: String,
                            descriptions: String, args: T*) extends AbstractCliCmd

object AbstractCliCmd {

  object Exit extends CliCmdWithoutArg("exit", "", "shutdown the SMR system.")

  object Help extends CliCmdWithoutArg("help", "", "show cli commands list.")

  case class KillServer(id: ServerId) extends AbstractCliCmd {
    override def command: ServerId = KillServer.cmd

    override def arguments: ServerId = "<server>"

    override def descriptions: ServerId = "shutdown a single server replica for simulating fault."
  }

  object ProfileServers extends CliCmdWithoutArg("prof", "", "show servers statuses.")

  object KillServer {
    val cmd = "kill"

    def getParamFromInputLine(inputLine: String) = inputLine.substring(KillServer.cmd.length + 1)
  }

  object Decrement extends CliCmdWithoutArg("dec", "", "decrement the value of the distributed counter.")

  object Increment extends CliCmdWithoutArg("inc", "", "increment the value of the distributed counter.")

  object Reset extends CliCmdWithoutArg("reset", "", "reset the distributed counter.")

  object Value extends CliCmdWithoutArg("value", "", "get the value of the distributed counter.")

  object InvalidInput extends CliCmdWithoutArg("-", "-", "unrecognized command.")

  val commands = Set(Exit, Help, KillServer("exampleId"), ProfileServers, Increment, Decrement, Reset, Value, InvalidInput)
}
