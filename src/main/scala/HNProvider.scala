package com.p15x.hntop30

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.{ Keep, Sink, Source }
import akka.stream.{ ActorMaterializer, OverflowStrategy }

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import cats.data.OptionT
import cats.implicits._
import org.slf4j.LoggerFactory
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.{Failure, Success}

trait HNProvider {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def ec: ExecutionContext

  def getTopStories(n: Int): Future[List[Story]]
}

class HNProviderImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends HNProvider {
  private val log = LoggerFactory.getLogger(classOf[HNProvider])
  private val hnApiBaseUri = "/v0"
  private val pool = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]]("hacker-news.firebaseio.com")
  private val queue = Source.queue[(HttpRequest, Promise[HttpResponse])](1000, OverflowStrategy.backpressure)
    .via(pool)
    .toMat(Sink.foreach({
      case ((Success(resp), p)) => p.success(resp)
      case ((Failure(e), p)) => p.failure(e)
    }))(Keep.left)
    .run

  def getTopStories(n: Int): Future[List[Story]] = {
    hnRequest[List[Int]]("topstories.json")
      .map(_.getOrElse(List()).take(n))
      .flatMap(getStories)
  }

  private def getStories(ids: List[Int]): Future[List[Story]] = {
    Future.sequence(ids.map(getStory)).map(_.flatten)
  }

  private def getStory(id: Int): Future[Option[Story]] = {
    val storyOT = for {
      story <- OptionT(getItem[HNStory](id))
      comments <- OptionT.liftF(getComments(story.kids.getOrElse(List())))
    }
    yield Story(story.id, story.title, comments)
    
    storyOT.value
  }

  private def getComments(ids: List[Int]): Future[List[Comment]] = {
    Future.sequence(ids.map(getComment)).map(_.flatten)
  }

  private def getComment(id: Int): Future[List[Comment]] = {
    // Remove comment without commenter as implictly deleted
    getHNComment(id).map(_.filter(_.by.isDefined).map(c => Comment(c.id, c.by.get)))
  }

  private def getHNComments(ids: List[Int]): Future[List[HNComment]] = {
    Future.sequence(ids.map(getHNComment)).map(_.flatten)
  }

  private def getHNComment(id: Int): Future[List[HNComment]] = {
    val commentsOT = for {
      comment <- OptionT(getItem[HNComment](id))
      comments <- OptionT.liftF(getHNComments(comment.kids.getOrElse(List())))
    }
    yield comment :: comments

    commentsOT.getOrElse(List())
  }

  private def getItem[T: Decoder](id: Int): Future[Option[T]] = {
    hnRequest[T](s"item/${id}.json")
  }

  private def hnRequest[T: Decoder](endpoint: String)(implicit ec: ExecutionContext, materializer: ActorMaterializer): Future[Option[T]] = {
    val promise = Promise[HttpResponse]
    val request = HttpRequest(uri = s"${hnApiBaseUri}/${endpoint}") -> promise

    queue.offer(request)
      .flatMap{
        case QueueOfferResult.Enqueued    => promise.future
        case QueueOfferResult.Dropped     => Future.failed(new RuntimeException("Queue overflowed. Try again later."))
        case QueueOfferResult.Failure(ex) => Future.failed(ex)
        case QueueOfferResult.QueueClosed => Future.failed(new RuntimeException("Queue was closed (pool shut down) while running the request. Try again later."))
      }
      .flatMap { r => Unmarshal(r.entity).to[String] }
      .map { r => decode[T](r) }
      .map {
        case Right(t) =>
          Some(t)
        case Left(e) =>
          log.error(s"couldn't deserialize $endpoint", e)
          None
      }
  }

  def shutdown = Http().shutdownAllConnectionPools()
}

case class HNStory(id: Int, title: String, kids: Option[List[Int]])
case class HNComment(id: Int, by: Option[String], kids: Option[List[Int]])

case class Story(id: Int, title: String, comments: List[Comment])
case class Comment(id: Int, by: String)
