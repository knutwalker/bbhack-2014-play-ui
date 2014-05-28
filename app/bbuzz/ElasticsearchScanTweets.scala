package bbuzz

import bbuzz.support.ElasticsearchScanIterable
import scala.collection.immutable.Iterable

/**
 * Provides tweets by scanning an Elasticsearch index.
 *
 * All documents contained in a given index are expected to be valid tweets.
 * This uses the HTTP connection, not the internal transport.
 */
trait ElasticsearchScanTweets extends TweetProvider.FromStringIterable {

  /**
   * The hostname of the Elasticsearch server.
   */
  def host: String

  /**
   * The port of the HTTP endpoint of the Elasticsearch server.
   */
  def port: Int

  /**
   * The index that contains the tweets.
   */
  def index: String

  /**
   * Uses the [[http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/search-request-search-type.html#scan Elasticsearch Scan/Scroll API]] to obtain Tweets
   *
   * @return an Iterable of Strings, where every String is the JSON representation on a [[bbuzz.Tweet]]
   */
  final def iterable: Iterable[String] = {
    val es = s"http://$host:$port"
    ElasticsearchScanIterable(es, index, 10)
  }
}
