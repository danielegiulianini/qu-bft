package qu.controller

import qu.client.datastructures.DistributedCounter
import qu.model.ConcreteQuModel.{Key, Query, Request, Response, ServerId, emptyAuthenticatedRh, emptyOhs, emptyRh, nullAuthenticator}
import qu.model.{IncResult, QuorumSystemThresholds, ResetResult, SmrEventResult, SmrSystem, SmrSystemImpl, ValueResult}
import io.grpc.inprocess.InProcessServerBuilder
import qu.RecipientInfo
import qu.model.examples.Commands.Increment
import qu.service.AbstractQuService.{ServerInfo, jacksonSimpleQuorumServiceFactory}
import qu.service.datastructures.RemoteCounterServer
import qu.view.View
import qu.view.console.ConsoleView

import java.util.Scanner
import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext
import scala.util.{Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

//object as it's a singleton
class ControllerImpl extends Controller {

  private val model: SmrSystem = new SmrSystemImpl()
  private val view: View = new ConsoleView

  view.setObserver(this)
  view.start()

  //todo really needed?
  override def start(): Unit = {} //model.

  override def quit(): Unit = {
    //avvisa di chiudere tutto e poi chiude il programma
    model.stop()
  }

  override def killServer(serverId: String): Unit = {
    view.result(model.killServer(serverId))
  }

  override def increment(): Unit = {
    view.result(model.increment())
  }
  override def decrement(): Unit = {
    view.result(model.decrement())
  }

  override def value(): Unit = {
    view.result(model.value())
  }

  override def reset(): Unit = {
    view.result(model.reset())
  }

}