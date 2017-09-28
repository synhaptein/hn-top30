package com.p15x.hntop30

import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

trait HNProtocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val HNStoryFormat = jsonFormat3(HNStory)
  implicit val HNCommentFormat = jsonFormat3(HNComment)
}
