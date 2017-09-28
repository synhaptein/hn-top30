package com.p15x.hntop30

import scala.concurrent.{ ExecutionContext, Future }

trait StoryService {
  implicit def ec: ExecutionContext

  def getCommentersCounts(nbStories: Int, nbTopCommenters: Int): Future[List[(Story, List[CommenterCountWithTotal])]]
}

class StoryServiceImpl(hnProvider: HNProvider)(implicit val ec: ExecutionContext) extends StoryService {
  def getCommentersCounts(nbStories: Int, nbTopCommenters: Int): Future[List[(Story, List[CommenterCountWithTotal])]] = {
    hnProvider.getTopStories(nbStories).map { stories =>
      val commentersCountsByStory = getCommentersCountsByStory(stories, nbTopCommenters)
      val countsByCommenter = getCountsByCommenter(commentersCountsByStory)

      commentersCountsByStory.map { case (story, counts) =>
        story -> counts.map { count => 
          CommenterCountWithTotal(count.by, count.count, countsByCommenter.getOrElse(count.by, -1))
        }
      }
    }
  }

  private def getCommentersCountsByStory(stories: List[Story], nbTopCommenters: Int): List[(Story, List[CommenterCount])] = {
    stories.map { story =>
      story -> story.comments
        .groupBy(_.by)
        .map { case (by, comments) => CommenterCount(by, comments.length) }
        .toList
        .sortBy(-_.count)
        .take(nbTopCommenters)
    }
  }

  private def getCountsByCommenter(commentersCountsByStory: List[(Story, List[CommenterCount])]): Map[String, Int] = {
    commentersCountsByStory
      .flatMap { case (_, counts) => counts }
      .groupBy(_.by)
      .map { case (commenter, counts) => (commenter, counts.map(_.count).sum) }
      .toMap
  }   
}

case class CommenterCount(by: String, count: Int)
case class CommenterCountWithTotal(by: String, count: Int, totalCount: Int)
case class CommentersCounts(commentersCountsByStory: List[(Story, List[CommenterCount])], countsByCommenter: Map[String, Int])
