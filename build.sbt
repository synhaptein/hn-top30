scalaVersion := "2.12.3"

name := "hn-top30"
organization := "com.p15x"
version := "1.0"

libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-http"            % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "com.typesafe.akka" %% "akka-stream"          % "2.5.4",
  "com.typesafe.akka" %% "akka-actor"           % "2.5.4",
  "org.typelevel"     %% "cats-core"            % "1.0.0-MF",
  "ch.qos.logback"    %  "logback-classic"      % "1.2.3",

  "org.scalamock"     %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "org.scalatest"     %% "scalatest"                   % "3.0.1" % "test")

