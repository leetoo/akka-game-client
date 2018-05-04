import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.scalatest.{FunSuite, Matchers}

class ClientTest extends FunSuite with Matchers  {
  /**
    * use ~testQuick in sbt terminal for continuous testing
    * https://doc.akka.io/docs/akka-http/current/client-side/websocket-support.html
    */
  // test

  // Future[Done] is the materialized value of Sink.foreach,
  // emitted when the stream completes
  val incoming = Sink.foreach[Message] {

    case message: TextMessage.Strict =>      println(message.text)
  }

  // send this as a message over the WebSocket
  val outgoing = Source.single(TextMessage("hello world!"))

  // flow to use (note: not re-usable!)
  val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest("ws://echo.websocket.org"))

  // the materialized value is a tuple with
  // upgradeResponse is a Future[WebSocketUpgradeResponse] that
  // completes or fails when the connection succeeds or fails
  // and closed is a Future[Done] with the stream completion from the incoming sink
  val (upgradeResponse, closed) =
  outgoing
    .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
    .toMat(incoming)(Keep.both) // also keep the Future[Done]
    .run()

}



