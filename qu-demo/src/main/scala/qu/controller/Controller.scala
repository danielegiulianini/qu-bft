package qu.controller

import qu.view.ViewObserver

trait Controller extends ViewObserver {

  //all void return methods
  def killServer(serverId: String): Unit

  def getServersStatus(): Unit


  def start(): Unit

  def quit(): Unit


  //or perform(Operation)
  def increment(): Unit

  def decrement(): Unit

  def value(): Unit

  def reset(): Unit




  //continuing to send empty rh (leverage a stub)
  //def makeServerByzantine(serverId:String): Unit

}
