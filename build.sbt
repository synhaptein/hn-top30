scalaVersion := "2.12.4"

name := "hn-top30"
organization := "com.p15x"
version := "1.0"

libraryDependencies ++=  Seq(
  "com.typesafe.akka" %% "akka-http"            % "10.0.10",
  "com.typesafe.akka" %% "akka-stream"          % "2.5.4",
  "com.typesafe.akka" %% "akka-actor"           % "2.5.4",

  "org.typelevel"     %% "cats-core"            % "1.0.0-RC1",
  "io.circe"          %% "circe-core"           % "0.9.0-M2",
  "io.circe"          %% "circe-generic"        % "0.9.0-M2",
  "io.circe"          %% "circe-parser"         % "0.9.0-M2",

  "ch.qos.logback"    %  "logback-classic"      % "1.2.3",

  "org.scalamock"     %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "org.scalatest"     %% "scalatest"                   % "3.0.1" % "test")

