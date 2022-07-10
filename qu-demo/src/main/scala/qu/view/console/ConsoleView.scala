package qu.view.console

import qu.controller.Controller
import qu.model.ConcreteQuModel.ServerId
import qu.model.ServerStatus.ACTIVE
import qu.model._
import qu.view.View
import qu.view.console.AbstractCliCmd.KillServer.getParamFromInputLine
import qu.view.console.AbstractCliCmd.{InvalidInput, KillServer, commands}
import qu.view.console.AbstractCliCmd._
import qu.view.console.StringUtils.{concatenateByNewLine, padRight}

import java.util.Scanner
import scala.util.{Failure, Success, Try}

class ConsoleView extends View {

  var observer: Controller = _

  override def start(): Unit = {

    var stop = false
    val myScanner = new Scanner(System.in)

    println(title)
    println(generalPrompt)

    while (!stop && myScanner.hasNextLine) {
      ConsoleView.parse(myScanner.nextLine()) match {
        case Exit => println("quitting...")
          stop = true
          observer.quit()
        case Help => println(generalPrompt)
        case KillServer(id) => observer.killServer(id)
        case ProfileServers => observer.getServersStatus()
        case Increment => observer.increment()
        case Decrement => observer.decrement()
        case Reset => observer.reset()
        case Value => observer.value()
        case InvalidInput | _ => result(Failure(UnrecognizedCommand()))
      }
    }
  }

  val title = "Q/U protocol example SMR System"

  val generalPrompt: String = {
    val widthLength = 20
    val header = concatenateByNewLine("commands summary:", padRight("command", widthLength) + padRight("argument", widthLength) + padRight("description", widthLength)) // f"commands summary:\n$name%-20s$arguments%-20s$description\n"
    concatenateByNewLine(header,
      concatenateByNewLine(commands.filterNot(_ == InvalidInput).map(e =>
        padRight(e.command, widthLength) + padRight(e.arguments, widthLength) + padRight(e.descriptions, widthLength)
      )))
  }

  override def setObserver(controller: Controller): Unit = observer = controller

  override def result(result: Try[SmrEventResult]): Unit = println({

    val operationOk = "operation completed correctly. "
    val operationNotOk = "a problem raised up. "

    def printStatuses(serversStatuses: Map[ServerId, ServerStatus]): String = "Servers statuses are: \n" +
      serversStatuses.view.mapValues(status => if (status == ACTIVE) "active" else "shutdown").mkString("\n")

    result match {
      case Success(ValueResult(value)) =>
        operationOk + "Updated counter value is: " + value
      case Success(ServerKilled(id, serversStatuses)) =>
        operationOk + "Server " + id + " stopped. " + printStatuses(serversStatuses)
      case Success(ServersProfiled(serversStatuses)) => operationOk + printStatuses(serversStatuses)
      case Failure(ThresholdsExceededException(msg)) => operationNotOk + msg
      case Failure(ServerNotExistingException(msg)) => operationNotOk + msg
      case Failure(ServerAlreadyKilledException(msg)) => operationNotOk + msg
      case Failure(UnrecognizedCommand()) => "command not recognized. Please attain to the syntax, digit " + Help.command + " to display commands."
      case Failure(_) => "a problem raised up."
      case _ => operationOk
    }
  })
}


object ConsoleView {

  def parse(inputLine: String): AbstractCliCmd = {
    commands.find(cmd => inputLine.startsWith(cmd.command)) match {
      case Some(KillServer(_)) if getParamFromInputLine(inputLine) /*.toIntOption*/ .isEmpty => InvalidInput
      case Some(KillServer(_)) /*if getParamFromInputLine(inputLine).toIntOption.isDefined*/ => KillServer(getParamFromInputLine(inputLine))
      case Some(cmd) => cmd
      case _ => InvalidInput
    }
  }

}
