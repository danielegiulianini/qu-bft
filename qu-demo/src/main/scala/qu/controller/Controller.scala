package qu.controller

import qu.view.ViewObserver

/**
 * A controller for [[qu.model.SmrSystem]] and [[qu.view.View]] following Model-View-Controller architectural pattern.
 */
trait Controller extends ViewObserver {

  def start(): Unit

  def quit(): Unit

  //cluster management operations
  def killServer(serverId: String): Unit

  def getServersStatus(): Unit





  //distributed counter operations:
  def increment(): Unit

  def decrement(): Unit

  def value(): Unit

  def reset(): Unit

}
