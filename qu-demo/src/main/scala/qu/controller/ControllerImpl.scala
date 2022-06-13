package qu.controller

import qu.client.datastructures.DistributedCounter
import qu.model.ConcreteQuModel.{Key, Query, Request, Response, ServerId, emptyAuthenticatedRh, emptyOhs, emptyRh, nullAuthenticator}
import qu.model.{IncResult, QuorumSystemThresholds, ResetResult, SmrEventResult, SyncSmrSystem, SyncSmrSystemImpl, ValueResult}
import io.grpc.inprocess.InProcessServerBuilder
import qu.RecipientInfo
import qu.stub.client.StubFactories.inNamedProcessJacksonStubFactory
import qu.model.examples.Commands.Increment
import qu.service.AbstractQuService.{ServerInfo, jacksonSimpleQuorumServiceFactory}
import qu.service.datastructures.RemoteCounterServer
import qu.view.{ConsoleView, View}

import java.util.Scanner
import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

//object as it's a singleton
object ControllerImpl extends Controller {

  private val model: SyncSmrSystem = new SyncSmrSystemImpl()
  private val view: View = new ConsoleView

  view.setObserver(this)

  //todo really needed?
  override def start(): Unit = {} //model.

  override def quit(): Unit = {
    //avvisa di chiudere tutto e poi chiude il programma
    model.stop()
  }

  override def killServer(serverId: String): Unit = {
    //ritorna una future e poi thread comunico alla view... altrimenti blocco la ricezione del'input...
    view.result(model.killServer(serverId))
  }

  override def increment(): Unit = {
    /*async version: model.increment().onComplete({
      case Success(_) => view.
    })*/
    view.result(model.increment())
  }

  override def value(): Unit = {
    view.result(model.value())
  }

  override def reset(): Unit = {
    view.result(model.reset())
  }
  // override def decrement(): Unit = ???
  //override def makeServerByzantine(serverId: String): Unit = {}

}

object ProvaCaching extends App {
  val myScanner = new Scanner(System.in)

  // Show prompt
  // this.generalPrompt
  println("il tostring dell'increment: " + Increment)
  while (myScanner.hasNextLine) {
    var inputLine = myScanner.nextLine()

    println("I've read: " + inputLine)
    Thread.sleep(5000)
  }
}

/*private val authServer = ???

    private val serversScenario = ???

    private val counter: DistributedCounter = ???

   override def killServer(serverId: String): Unit = servers(serverId)

    override def start(): Unit = ???

    override def quit(): Unit = {
      counter.shutdown()

    }



    override def increment(): Unit = ???

    override def decrement(): Unit = ???

    override def value(): Unit = ???

    override def reset(): Unit = ???

  override def makeServerByzantine(serverId: String): Unit = ???*/