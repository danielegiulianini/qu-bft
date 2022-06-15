package qu.view

import qu.controller.Controller
import qu.model.SmrEventResult

import scala.util.Try

trait View {
  def start():Unit
  def setObserver(controller: Controller)
  def result(result: Try[SmrEventResult])
}
