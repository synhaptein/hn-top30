package com.p15x.hntop30

import cats.effect._
import cats.data.OptionT
import cats.implicits._
import org.http4s.circe._
import org.http4s.client.blaze._
import io.circe._, io.circe.generic.auto._
import org.slf4j.LoggerFactory
import scala.concurrent.{ ExecutionContext, Future }

trait HNProvider {
  implicit def ec: ExecutionContext

  def getTopStories(n: Int): Future[List[Story]]
}

class HNProviderImpl(implicit val ec: ExecutionContext) extends HNProvider {
  private val logger = LoggerFactory.getLogger(classOf[HNProvider])
  private val hnApiBaseUri = "https://hacker-news.firebaseio.com/v0"

  private val httpClient = PooledHttp1Client[IO](
    maxTotalConnections = 64,
    maxWaitQueueLimit = 1000,
    maxConnectionsPerRequestKey = _ => 64)

  def getTopStories(n: Int): Future[List[Story]] = {
    hnRequest[List[Int]]("topstories.json")
      .map(_.getOrElse(List()).take(n))
      .flatMap(getStories)
      .unsafeToFuture()
  }

  private def getStories(ids: List[Int]): IO[List[Story]] = {
    fs2.async.parallelSequence(ids.map(getStory)).map(_.flatten)
  }

  private def getStory(id: Int): IO[Option[Story]] = {
    val storyOT = for {
      story <- OptionT(getItem[HNStory](id))
      comments <- OptionT.liftF(getComments(story.kids.getOrElse(List())))
    }
    yield Story(story.id, story.title, comments)
    
    storyOT.value
  }

  private def getComments(ids: List[Int]): IO[List[Comment]] = {
    fs2.async.parallelSequence(ids.map(getComment)).map(_.flatten)
  }

  private def getComment(id: Int): IO[List[Comment]] = {
    // Remove comment without commenter as implictly deleted
    getHNComment(id).map(_.filter(_.by.isDefined).map(c => Comment(c.id, c.by.get)))
  }

  private def getHNComments(ids: List[Int]): IO[List[HNComment]] = {
    fs2.async.parallelSequence(ids.map(getHNComment)).map(_.flatten)
  }

  private def getHNComment(id: Int): IO[List[HNComment]] = {
    val commentsOT = for {
      comment <- OptionT(getItem[HNComment](id))
      comments <- OptionT.liftF(getHNComments(comment.kids.getOrElse(List())))
    }
    yield comment :: comments

    commentsOT.getOrElse(List())
  }

  private def getItem[T: Decoder](id: Int): IO[Option[T]] = {
    hnRequest[T](s"item/${id}.json")
  }

  private def hnRequest[T: Decoder](endpoint: String): IO[Option[T]] = {
    httpClient.expect(s"${hnApiBaseUri}/${endpoint}")(jsonOf[IO, T])
      .map(_.some)
      .recover { case e: Throwable =>
        logger.error(s"Could not deserialize response on $endpoint", e)
        None
      }
  }

  def shutdown = httpClient.shutdownNow()
}

case class HNStory(id: Int, title: String, kids: Option[List[Int]])
case class HNComment(id: Int, by: Option[String], kids: Option[List[Int]])

case class Story(id: Int, title: String, comments: List[Comment])
case class Comment(id: Int, by: String)
