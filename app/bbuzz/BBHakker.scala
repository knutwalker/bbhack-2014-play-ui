package bbuzz

import akka.actor.{Props, Actor, ActorRef}
import play.api.libs.json.{JsArray, Json}
import scala.concurrent.duration._

class BBHakker(out: ActorRef, data: BBHack, top: Int) extends Actor {
  import BBHakker._
  import context.dispatcher

  var th: Thread = _

  implicit val topTagFormat = Json.format[TopHashTag]

  override def preStart(): Unit = {
    super.preStart()
    context.system.scheduler.schedule(100.millis, 100.millis, self, Summary)
    th = new Thread(new Runnable{ def run() = data.main(Array())})
    th.start()
  }

  override def postStop(): Unit = {
    th.stop()
    super.postStop()
  }

  def summary(top: Int) = data.topK().take(top).map {
    case (ht, cnt) => TopHashTag(ht, cnt, data.sketchGet(ht)) }

  def serialize(xs: List[TopHashTag]) =
    JsArray(xs.map(Json.toJson(_)))

  def receive = {
    case Summary => out ! serialize(summary(top))
  }
}
object BBHakker {
  def props(out: ActorRef, data: BBHack, top: Int) = Props(new BBHakker(out, data, top))
  case object Summary
  case class TopHashTag(tag: String, topKCount: Int, sketchCount: Int)
}
