name := """zencroissants"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.11",
  "org.julienrf" %% "enum" % "3.0",
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  ws
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

routesGenerator := InjectedRoutesGenerator
