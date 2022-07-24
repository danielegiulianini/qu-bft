package qu.view

import qu.controller.Controller
import qu.model.SmrEventResult

import scala.util.Try

/**
 * Abstract modelling of a view, that could actually be a command-line or a graphic user interface.
 */
trait View {
  def start():Unit
  def setObserver(controller: Controller): Unit
  def result(result: Try[SmrEventResult]): Unit
}
