scalaVersion := "2.12.4"

name := "hn-top30"
organization := "com.p15x"
version := "1.0"

val circeVersion = "0.9.0-M2"
val http4sVersion = "0.18.0-M5"

libraryDependencies ++=  Seq(
  "org.http4s"        %% "http4s-dsl"           % http4sVersion,
  "org.http4s"        %% "http4s-blaze-client"  % http4sVersion,
  "org.http4s"        %% "http4s-circe"         % http4sVersion,

  "org.typelevel"     %% "cats-core"            % "1.0.0-RC1",
  "io.circe"          %% "circe-core"           % circeVersion,
  "io.circe"          %% "circe-generic"        % circeVersion,
  "io.circe"          %% "circe-parser"         % circeVersion,

  "ch.qos.logback"    %  "logback-classic"      % "1.2.3",

  "org.scalamock"     %% "scalamock-scalatest-support" % "3.6.0" % "test",
  "org.scalatest"     %% "scalatest"                   % "3.0.1" % "test")

