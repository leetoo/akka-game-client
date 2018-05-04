import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.{FunSuite, Matchers}

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
    val input = Source.actorRef[String](5,OverflowStrategy.dropNew)
    val output = TestSink.probe[List[Player]]
    val ((inputMat,result),outputMat) = client.run(input,output)
    inputMat ! "up"
    outputMat.request(2)
    outputMat.expectNext(List(Player("Dennis",Position(0,0))))
    outputMat.expectNext(List(Player("Dennis",Position(0,2))))

  }
}

case class Client(playerName:String)(implicit val actorSystem:ActorSystem, implicit val
actorMaterializer:ActorMaterializer){
 import spray.json._
  import DefaultJsonProtocol._
  implicit val positionFormat = jsonFormat2(Position)
  implicit val playerFormat = jsonFormat2(Player)
  val webSocketFlow =
    Http().webSocketClientFlow(WebSocketRequest(s"ws://localhost:8080/?playerName=$playerName")).collect{
      case TextMessage.Strict(strMsg) => strMsg.parseJson.convertTo[List[Player]]
    }
  def run[M1,M2](input:Source[String,M1], output :Sink[List[Player], M2] )= {
    input.map(direction => TextMessage(direction) )
      .viaMat(webSocketFlow)(Keep.both) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(output)(Keep.both) // also keep the Future[Done]
      .run()
  }
}
case class Player(name:String, position:Position)
case class Position(x:Int , y:Int )