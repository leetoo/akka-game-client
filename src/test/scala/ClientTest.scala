
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.scalatest.{FunSuite, Matchers}
import scalafx.application.JFXApp
import scalafx.scene.Scene
import scalafx.scene.input.{KeyCode, KeyEvent}

class ClientTest extends FunSuite with Matchers {
  /**
    * use ~testQuick in sbt terminal for continuous testing
    * https://doc.akka.io/docs/akka-http/current/client-side/websocket-support.html
    */
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  test("should be able to login player  ") {
    val testSink = TestSink.probe[Message]
    val outgoing = Source.empty[Message]
    val webSocketFlow =
      Http().webSocketClientFlow(WebSocketRequest("ws://localhost:8080/?playerName=dennis"))
    val (upgradeResponse, testProbe) =
      outgoing
        .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
        .toMat(testSink)(Keep.both) // also keep the Future[Done]
        .run()
    testProbe.request(1)
    testProbe.expectNext(TextMessage("[{\"name\":\"dennis\",\"position\":{\"x\":0," +
      "\"y\":0}}]"))
  }
  test("should be able to move player ") {
    val client = new Client("Dennis")
    // output = List[players]
    val input = Source.actorRef[String](5, OverflowStrategy.dropNew)
    val output = TestSink.probe[List[Player]]
    val ((inputMat, result), outputMat) = client.run(input, output)
    inputMat ! "up"
    outputMat.request(2)
    outputMat.expectNext(List(Player("Dennis", Position(0, 0))))
    outputMat.expectNext(List(Player("Dennis", Position(0, 1))))
  }
}
object Main {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val client = new Client("Denis")
    val display = new Display()
    val input = Source.actorRef[String](5, OverflowStrategy.dropNew)
    val output = display.sink
    val ((inputMat, result), outputMat) = client.run(input, output)
    val keyBoardHandler = new KeyBoardHandler(inputMat)
    new GUI(keyBoardHandler, display).main(args)

  }
}
case class Client(playerName: String)(implicit val actorSystem: ActorSystem, implicit val
actorMaterializer: ActorMaterializer) {
  import spray.json._
  import DefaultJsonProtocol._

  implicit val positionFormat = jsonFormat2(Position)
  implicit val playerFormat = jsonFormat2(Player)
  val webSocketFlow =
    Http().webSocketClientFlow(WebSocketRequest(s"ws://localhost:8080/?playerName=$playerName")).collect {
      case TextMessage.Strict(strMsg) => strMsg.parseJson.convertTo[List[Player]]
    }
  def run[M1, M2](input: Source[String, M1], output: Sink[List[Player], M2]) = {
    input.map(direction => TextMessage(direction))
      .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(output)(Keep.both) // also keep the Future[Done]
      .run()
  }
}
case class Player(name: String, position: Position)
case class Position(x: Int, y: Int)
class KeyBoardHandler(keyboardEventReceiver: ActorRef) {
  def handle(keyEvent: KeyEvent) = keyEvent.code match {
    case KeyCode.Up => keyboardEventReceiver ! "up"
    case KeyCode.Down => keyboardEventReceiver ! "down"
    case KeyCode.Left => keyboardEventReceiver ! "left"
    case KeyCode.Right => keyboardEventReceiver ! "right"
  }
}
import akka.stream.scaladsl.Sink
import scalafx.application.Platform
import scalafx.scene.layout.{AnchorPane, StackPane}
import scalafx.scene.paint.Color
import scalafx.scene.shape.Circle
import scalafx.scene.text.Text

class Display() {
  private val PlayerRadius = 100
  private val Dimensions = 6
  private val ScreenSize = PlayerRadius * Dimensions
  val panel = new AnchorPane {
    minWidth = ScreenSize
    minHeight = ScreenSize
  }
  def sink = Sink.foreach[List[Player]] { playerPositions =>
    val playersShapes = playerPositions.map(player => {
      new StackPane {
        minWidth = ScreenSize
        minHeight = ScreenSize
        layoutX = player.position.x * PlayerRadius
        layoutY = player.position.y * PlayerRadius
        prefHeight = PlayerRadius
        prefWidth = PlayerRadius
        val circlePlayer = new Circle {
          radius = PlayerRadius * 0.5
          fill = getColorForPlayer(player.name)
        }
        val textOnCircle = new Text {
          text = player.name
        }
        children = Seq(circlePlayer, textOnCircle)
        def getColorForPlayer(name: String) = {
          val r = 55 + math.abs(("r" + name).hashCode) % 200
          val g = 55 + math.abs(("g" + name).hashCode) % 200
          val b = 55 + math.abs(("b" + name).hashCode) % 200
          Color.rgb(r, g, b)
        }
      }
    })
    Platform.runLater({
      panel.children = playersShapes
      panel.requestLayout()
    })
  }
}
class GUI(keyBoardHandler: KeyBoardHandler, display: Display) extends JFXApp {
  import scalafx.Includes._

  stage = new JFXApp.PrimaryStage {
    title.value = "client"
    scene = new Scene {
      scene = new Scene {
        content = display.panel
        onKeyPressed = (ev: KeyEvent) => keyBoardHandler.handle(ev)
      }
    }
  }
}