package com.akkagame.gui

import akka.actor.ActorRef
import scalafx.scene.input.{KeyCode, KeyEvent}

class KeyBoardHandler(keyboardEventReceiver: ActorRef) {
  def handle(keyEvent: KeyEvent) = keyEvent.code match {
    case KeyCode.Up => keyboardEventReceiver ! "down" // "down"  // scala coordinate is reversed
    case KeyCode.Down => keyboardEventReceiver ! "up" // "up"
    case KeyCode.Left => keyboardEventReceiver ! "right" // "right"
    case KeyCode.Right => keyboardEventReceiver ! "left" // "left"
    case _ => // solved run-time exception
  }
}
