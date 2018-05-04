package com.akkagame.gui

import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.input.KeyEvent

class GUI(keyBoardHandler: KeyBoardHandler, display: Display) extends JFXApp {
  import scalafx.Includes._

  stage = new JFXApp.PrimaryStage {
    title.value = "client"
    scene = new Scene {
      content = display.panel
      onKeyPressed = (ev: KeyEvent) => keyBoardHandler.handle(ev)
    }
  }
}
