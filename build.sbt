ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.2"

lazy val root = (project in file("."))
  .settings(
    name := "GameJamsScala"
  )

val json4sJackson = "org.json4s" %% "json4s-jackson" % "4.0.6"
val json4sNative = "org.json4s" %% "json4s-native" % "4.0.6"
libraryDependencies += json4sNative
libraryDependencies += json4sJackson