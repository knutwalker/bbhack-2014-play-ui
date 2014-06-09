package controllers

import bbuzz.{BBHack, BBHakker, ElasticsearchScanTweets}
import play.api.mvc.{Controller, Action, WebSocket}
import play.api.libs.json.JsValue

trait Elastic extends ElasticsearchScanTweets {
  def host = "localhost"
  def port = 9200
  def index = "bbuzz"
}

object Application extends Controller {
  import BBHakker.TopHashTag
  import play.api.Play.current

  def smData = new BBHack(maxK = 100, bucketSize = 10000, hashSize = 15) with Elastic

  def index = Action {
    Ok(views.html.index())
  }

  def ws = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    BBHakker.props(out, smData, 10)
  }
}
