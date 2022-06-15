package qu.view.console

import qu.controller.Controller
import qu.model.ConcreteQuModel.ServerId
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

    while (myScanner.hasNextLine && !stop) {
      ConsoleView.parse(myScanner.nextLine()) match {
        case Exit => println("quitting")
          stop = true
          observer.quit()
        case Help => println(generalPrompt)
        case KillServer(id) => observer.killServer(id)
        case Increment => observer.increment()
        case Decrement => observer.reset()
        case Reset => observer.reset()
        case Value => observer.value()
        case InvalidInput => result(Failure(UnrecognizedCommand()))
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
    /*def printValue =
    def printStatus(serversStatuses: Map[ServerId, ServerStatus]) = operationOk + "Servers " + id + "stopped. Servers status are: " + serversStatuses
*/
    result match {
      case Success(ValueResult(value)) =>
        operationOk + "Updated counter value is: " + value
      case Success(ServerKilled(id, serversStatuses)) =>
        operationOk + "Servers " + id + "stopped. Servers status are: " + serversStatuses
      case Failure(ThresholdsExceededException(msg)) => msg
      case Failure(ServerAlreadyKilledException(msg)) => msg
      case Failure(UnrecognizedCommand()) => "command not recognized. Please attain to the syntax, digit help to display commands."
      case Failure(_) => "a problem raised up."
      case _ => operationOk
    }
  })
}



object ConsoleView {

  def parse(inputLine: String): AbstractCliCmd = {
    commands.find(cmd => inputLine.startsWith(cmd.command)) match {
      case Some(KillServer(_)) if getParamFromInputLine(inputLine).toIntOption.isEmpty => InvalidInput
      case Some(KillServer(_)) if getParamFromInputLine(inputLine).toIntOption.isDefined => KillServer(getParamFromInputLine(inputLine))
      case Some(cmd) => cmd
      case _ => InvalidInput
    }
  }

}


/*Non rifattorizzato:

Con la mappa anziché il MSg
  case class CommandDescription(command: String, arguments: String, description: String)

  val commands = Set[CommandDescription](
    CommandDescription("inc", "", "increment a value of the counter"),
    CommandDescription("dec", "", "increment a value of the counter"),
    CommandDescription("dec", "", "increment a value of the counter"),
    CommandDescription("res", "", "increment a value of the counter"),
    CommandDescription("exit", "", "increment a value of the counter"),
    CommandDescription("kill", "<serverId>", "increment a value of the counter"),
  )


while (myScanner.hasNextLine && !stop) {
      // Scan next line from command-prompt
      var inputLine = myScanner.nextLine()
      // Deceide which operation to do...
      if (inputLine == "insert") {
        //Console.synchronized {
        println()
        //}
        //this.controller.insertOperation(query)
      }
      else if (inputLine == commands1(Cmd.Exit).command) { //inputLine == "udpate") {
      }
      else if (inputLine == "quit") { // Bye bye...
        stop = true
      }
      else {
        println("command not recognized...")
      } //this.generalPrompt      // Display Prompt
    }
*

   // header + commands.map { case CliCmd(desc, ss, ss2) => f"$desc%-20s$ss%-20s$ss2" }.mkString("\n")


def parse(inputLine: String): AbstractCliCmd = {
    commands.filter(cmd => inputLine.startsWith(cmd.command)).headOption match {
      case Some(KillServer(_)) if getParamFromInputLine(inputLine).toIntOption.isEmpty => {
        println("è un killserver invalido, la  getParamFromInputLine(inputLine) da:  " + getParamFromInputLine(inputLine))
        InvalidInput
      }
      case Some(KillServer(_)) if getParamFromInputLine(inputLine).toIntOption.isDefined => {
        println("è un killserver valido")

        KillServer(getParamFromInputLine(inputLine))
      }
      case Some(cmd) => {
        println("è un altro cmd valido")

        cmd
      }
      case _ => {
        println("è un onvalido")

        InvalidInput
      }
    }
    }

* */


/*
* Alternativa con if- else
*
*     /*if (inputLine.startsWith(Exit.command)) AbstractCliCmd.Exit
    else if (inputLine.startsWith(Exit.command)) AbstractCliCmd.KillServer("e")
    else if (inputLine.startsWith(KillServer.command)) try {
    inputLine.substring(KillServer.cmd.length) KillServer("e") //oppure intoption
    } catch (exception) {
     InvalidInput
    }
    else if (inputLine.startsWith(Exit.command)) AbstractCliCmd.KillServer("e")
    else if (inputLine.startsWith(Exit.command)) AbstractCliCmd.KillServer("e")
    else AbstractCliCmd.InvalidInput*/

    */

/* def problem(ex: Exception): Unit = println({
     ex match {
       case _: ThresholdsExceededException => "problem..."
       case _ => "problem..."
     }
   })*/
