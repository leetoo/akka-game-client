import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.testkit.scaladsl.TestSink
import org.scalatest.{FunSuite, Matchers}

class ClientTest extends FunSuite with Matchers {
  /**
    * use ~testQuick in sbt terminal for continuous testing
    * https://doc.akka.io/docs/akka-http/current/client-side/websocket-support.html
    */
  test("should be able to login player  ") {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
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
}



