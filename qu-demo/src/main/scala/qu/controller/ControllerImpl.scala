package qu.controller

import qu.model.{SmrSystem, SmrSystemImpl}
import qu.view.View
import qu.view.console.ConsoleView


/**
 * An simple implementation of [[Controller]].
 */
class ControllerImpl extends Controller { //could be a singleton

  private val model: SmrSystem = new SmrSystemImpl()
  private val view: View = new ConsoleView

  view.setObserver(this)
  view.start()

  override def start(): Unit = {}

  override def quit(): Unit = {
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

  override def getServersStatus(): Unit = view.result(model.getServersStatus)
}