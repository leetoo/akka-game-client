name := "akka-stream-game-server-server"

version := "1.0"

scalaVersion := "2.12.4"

val akkaV = "2.4.10"

libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-http-core" % akkaV,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaV % "test",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaV,
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)

