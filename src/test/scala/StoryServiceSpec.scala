package com.p15x.hntop30

import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Random

class StoryServiceSpec extends UnitSpec {
  "An empty story list" should "return empty commenters counts" in {
    val hnProvider = mock[HNProvider]
    hnProvider.expects('getTopStories)(1).returning(Future.successful(List[Story]()))

    val storyService: StoryService = new StoryServiceImpl(hnProvider)

    val stories = Await.result(storyService.getCommentersCounts(1, 1), Duration.Inf)

    stories shouldBe empty
    stories.map(_._2).flatten shouldBe empty
  }

  "A non-empty story list without commenters" should "return non-empty story list and empty empty commenter counts" in {
    val nbStories = 5
    val withoutCommentersStories = (1 to nbStories).map(i => Story(i, s"user $i", List[Comment]())).toList

    val hnProvider = mock[HNProvider]
    hnProvider.expects('getTopStories)(nbStories).returning(Future.successful(withoutCommentersStories))

    val storyService: StoryService = new StoryServiceImpl(hnProvider)

    val stories = Await.result(storyService.getCommentersCounts(nbStories, 1), Duration.Inf)

    stories should have length nbStories
    stories.map(_._2).flatten shouldBe empty
  }

  "A story list with commenters" should "return a story list with top N commenters in descending order for each story" in {
    val nbStories = 20
    val nbCommenters = 3
    val nbUniqueCommenters = 6

    val mockStories = (0 until nbStories).map(i => {
      val comments = (1 to i).map(j => Comment(j, s"user ${j % nbUniqueCommenters}")).toList

      Story(i, s"title $i", Random.shuffle(comments))
    }).toList

    val hnProvider = mock[HNProvider]
    hnProvider.expects('getTopStories)(nbStories).returning(Future.successful(mockStories))

    val storyService: StoryService = new StoryServiceImpl(hnProvider)

    val stories = Await.result(storyService.getCommentersCounts(nbStories, nbCommenters), Duration.Inf)
    val commentersCounts = stories.map(_._2)
    val uniqueCommenters = commentersCounts.flatten.groupBy(_.by).keys

    stories should have length nbStories
    uniqueCommenters.size should be <= nbUniqueCommenters

    // make sure there's maximum nbCommenters per story
    all(commentersCounts.map(_.length)) should (be >= 0 and be <= nbCommenters)

    // make sure the top commenters are in descending order per story
    def isSortedDesc(l: List[Int]): Boolean = l match {
      case l if (l.size <= 1) => true
      case l => l.sliding(2).forall {
        case List(a, b) => a >= b
      }
    }

    forAll(commentersCounts.map(_.map(_.count))) { counts =>
      isSortedDesc(counts) shouldBe true
    }
  }

}
