package com.p15x.hntop30

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  implicit val ec = scala.concurrent.ExecutionContext.global

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
    hnProvider.shutdown
  }
}
