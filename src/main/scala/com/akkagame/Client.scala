package com.akkagame

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{TextMessage, WebSocketRequest}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.akkagame.domain.{Player, Position}

class Client(playerName: String)(implicit val actorSystem: ActorSystem, implicit val
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