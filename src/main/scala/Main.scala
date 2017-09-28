package com.p15x.hntop30

import scala.concurrent.Await
import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  val hnProvider = new HNProviderImpl
  val storyService = new StoryServiceImpl(hnProvider)

  try {
    val nbStories = 30
    val nbTopCommenters = 10

    val stories = Await.result(storyService.getCommentersCounts(nbStories, nbTopCommenters), Duration.Inf)

    def formatCommenterCount(count: CommenterCountWithTotal): String =
      s"${count.by} (${count.count} - ${count.totalCount} total)"

    val storiesDisplay = stories.map { case (story, counts) =>
      story.title :: DisplayHelper.fillsWith(counts.map(formatCommenterCount), "", nbTopCommenters)
    }

    val table = Tabulator.format(DisplayHelper.createHeader(nbTopCommenters) :: storiesDisplay)

    println(table)
  }
  finally {
    hnProvider.shutdown.map { _ =>
      system.terminate()
      Await.result(system.whenTerminated, 3 seconds)
    }
  }
}
