name := """ZenCroissants"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.12.1",
  "org.reactivemongo" %% "reactivemongo-play-json" % "0.12.1",
  "org.julienrf" %% "enum" % "3.0",
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1",
  ws
)

routesGenerator := InjectedRoutesGenerator
