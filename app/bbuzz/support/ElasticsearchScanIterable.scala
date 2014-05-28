package bbuzz.support

import dispatch._
import dispatch.Defaults._
import java.text.SimpleDateFormat
import java.util.Locale
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.collection.immutable.Iterable
import scala.util.{Failure, Success, Try}


class ElasticsearchScanIterable private(es: String, index: String, bulkSize: Int) extends Iterable[String] {

  import ElasticsearchScanIterable.ElasticsearchScanIterator

  def iterator: Iterator[String] = new ElasticsearchScanIterator(es, index, bulkSize)
}


object ElasticsearchScanIterable {

  private class ElasticsearchScanIterator(es: String, index: String, bulkSize: Int) extends Iterator[String] {
    implicit val formats = DefaultFormats

    private[this] val searchUrl = s"$es/$index/_search"
    private[this] val scanUrl = s"$es/_search/scroll"

    private[this] final def getNextCursor(res: JValue) = (res \ "_scroll_id").extract[String]

    private[this] final def execute(url: String, params: List[(String, String)]): JValue = {
      val svc = dispatch.url(url) <<? params
      parse(Http(svc OK as.String).apply())
    }

    private[this] final def getNextResult(res: JValue) = {
      val requestParams = List(("scroll", "10m"), ("scroll_id", getNextCursor(res)))
      execute(scanUrl, requestParams)
    }

    private[this] final val initialResult = {
      val requestParams = List("search_type" -> "scan", "scroll" -> "1m", "size" -> bulkSize.toString)
      execute(searchUrl, requestParams)
    }
    final val total = (initialResult \ "hits" \ "total").extract[Long]

    private[this] final val firstResult = getNextResult(initialResult)

    private[this] var result = firstResult


    private[this] def makeResultIterator() = {
      val JArray(elements) = result \ "hits" \ "hits"
      elements.toIterator.map { hit =>
        parseSource(hit \ "_source")
      }
    }

    private[this] var resultIterator = makeResultIterator()

    private[this] var finallyEmpty = false

    def hasNext: Boolean = synchronized {
      finallyEmpty || {
        if (!resultIterator.hasNext) fetchNext()
        val hasNextBatch = resultIterator.hasNext
        if (!hasNextBatch) finallyEmpty = true
        hasNextBatch
      }
    }

    def next(): String = synchronized {
      if (!hasNext) Iterator.empty.next()
      doNext()
    }

    private[this] def doNext() = {
      resultIterator.next()
    }

    private[this] def fetchNext() = {
      result = getNextResult(result)
      resultIterator = makeResultIterator()
    }
  }

  private val hagenFormat = {
    val f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
    f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    f
  }

  private val twitterFormat = {
    val f = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US)
    f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"))
    f
  }

  private def changeDateFormat(hagenDate: String): String = {
    Try(twitterFormat.format(hagenFormat.parse(hagenDate))).toOption.getOrElse(hagenDate)
  }

  private val entites = Set("user_mentions", "urls", "hashtags", "symbols", "media")

  private def filterMissingIndices(items: List[JValue]): JArray = JArray(items filter { item =>
    item \ "indices" match {
      case JArray(x :: y :: _) => true
      case _                   => false
    }
  })

  private def parseSource(source: JValue): String = {
    val newSource = source transformField {
      case JField("created_at", JString(hagenDate)) =>
        Try(changeDateFormat(hagenDate)) match {
          case Success(twitterDate) => "created_at" -> JString(twitterDate)
          case Failure(ex)          => ex.printStackTrace(); "hagen_created_at" -> JString(hagenDate)
        }
      case JField(x, JArray(items)) if entites(x)   =>
        x -> filterMissingIndices(items)
    }

    compact(render(newSource))
  }

  def apply(es: String, index: String, bulkSize: Int = 100): Iterable[String] = new ElasticsearchScanIterable(es, index, bulkSize)
}
