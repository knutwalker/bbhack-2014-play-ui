package controllers

import bbuzz._
import play.api._
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.Json
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future


case class TopHashTag(tag: String, topKCount: Int, sketchCount: Int)

object TopHashTag {
  implicit val js = Json.writes[TopHashTag]
}

class BBHack extends TweetStreaming
with HeavyHitters
with CMSketch
with Hashings {
  this: TweetProvider =>

  val bucketSize = 1000
  val hashSize = 3

  val maxK = 25

  var i = 0

  def handleException(exception: Throwable): Unit = exception.printStackTrace()

  def onTweet(tweet: bbuzz.Tweet): Unit = {
    i += 1
    Some(tweet.getHashtagEntities.map(_.getText))
      .getOrElse(Array())
      .foreach { ht =>
        insert(ht)
        sketchCount(ht)
      }
  }

  def summary(top: Int) = topK().take(top).map { case (ht, cnt) => TopHashTag(ht, cnt, sketchGet(ht)) }
}


object Application extends Controller {
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
  val counts = new BBHack with ZeroMq

  new Thread(new Runnable {
    def run(): Unit = counts.main(Array())
  }).start()

  def delayed = {
    Future {
      Thread.sleep(100)
      Json.stringify(Json.toJson(counts.summary(5)))
    }
  }

  def index = Action {
    Ok(views.html.index())
  }


  def ws = WebSocket.using[String] { request =>

    val in = Iteratee.foreach[String](println).map { _ =>
      println("Disconnected")
    }

    val out = Enumerator.repeat(Json.stringify(Json.toJson(counts.summary(5))))
//    val out = Enumerator.repeatM(delayed)

    (in, out)
  }

}
