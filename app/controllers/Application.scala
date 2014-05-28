package controllers

import akka.actor.{Props, Actor}
import akka.util.Timeout
import bbuzz._
import scala.concurrent.duration._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc._

import scala.concurrent.Future


abstract class BBHack(val maxK: Int, val bucketSize: Int, val hashSize: Int) extends TweetStreaming
with HeavyHitters with CMSketch with Hashings {
  this: TweetProvider =>

  def handleException(exception: Throwable): Unit = exception.printStackTrace()

  def onTweet(tweet: bbuzz.Tweet): Unit = {
    Some(tweet.getHashtagEntities.map(_.getText)).getOrElse(Array()).foreach { ht =>
      insert(ht)
      sketchCount(ht)
    }
  }
}

trait ZeroMq extends ZeroMqTweets {
  def host: String = ???
  def channel: String = "tweet.stream"
  def port: Int = 5555
}
trait Elastic extends ElasticsearchScanTweets {
  def host: String = ???
  def port: Int = 80
  def index: String = "bbuzz-hackday"
}

class HackActor(maxK: Int, bucketSize: Int, hashSize: Int) extends BBHack(maxK, bucketSize, hashSize) with Actor with Elastic {
  import HackActor._
  import context.dispatcher

  implicit val topTagWrites = Json.writes[TopHashTag]
  val delay = 25 millis

  Future { main(Array()) }

  def summary(top: Int) = topK().take(top).map { case (ht, cnt) => TopHashTag(ht, cnt, sketchGet(ht)) }

  def serialize(xs: List[TopHashTag]) = {
    Json.stringify(JsArray(xs.map(Json.toJson(_))))
  }

  def receive = {
    case Summary(top) =>
      val result = serialize(summary(top))
      context.system.scheduler.scheduleOnce(delay, sender, result)
  }
}

object HackActor {
  case class Summary(top: Int)
  case class TopHashTag(tag: String, topKCount: Int, sketchCount: Int)

  def props(maxK: Int, bucketSize: Int, hashSize: Int) = Props(classOf[HackActor], maxK, bucketSize, hashSize)
}


object Application extends Controller {
  import HackActor.Summary
  import play.api.Play.current
  import play.api.libs.concurrent.Akka
  import play.api.libs.concurrent.Execution.Implicits._
  import akka.pattern.ask

  implicit val askTimeout = Timeout(1 second)

  val hakker = Akka.system.actorOf(HackActor.props(10, 1000, 3))

  def index = Action {
    Ok(views.html.index())
  }

  def ws = WebSocket.using[String] { request =>

    val in = Iteratee.foreach[String](println).map { _ => println("Disconnected") }
    val out = Enumerator.repeatM(hakker.ask(Summary(5)).mapTo[String])

    (in, out)
  }

}
