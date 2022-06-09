package qu.controller

import qu.view.ViewObserver

trait Controller extends ViewObserver {
  def killServer(serverId:String): Unit

  //continuing to send empty rh (leverage a stub)
  def makeServerByzantine(serverId:String): Unit

  def start(): Unit

  def quit(): Unit


}
