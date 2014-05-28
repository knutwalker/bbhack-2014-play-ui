package bbuzz

import rx.lang.scala.schedulers.ComputationScheduler

/**
 * Consumes tweets of a stream.
 *
 * The user should extend this trait and implement `onTweet`
 * as well as `handleException`
 */
trait TweetConsumer extends TweetObserver {

  final override def onNext(value: Tweet): Unit = onTweet(value)
  final override def onError(error: Throwable): Unit = handleException(error)
  final override def onCompleted(): Unit = ()

  /**
   * Gets called for every tweet that is pulled from the [[bbuzz.TweetProvider]].
   * This method should not block and complete quickly.
   *
   * @param tweet a new [[Tweet]]
   */
  def onTweet(tweet: Tweet): Unit

  /**
   * Gets called when an exception occurred during providing the Tweets.
   * This will be a fatal exception, and tweets will not continue to flow after
   * this exception occurred.
   *
   * @param exception the fatal exception
   */
  def handleException(exception: Throwable): Unit
}

/**
 * Entry point for the app.
 *
 * 1. create an `object`, extending this (`TweetStreaming`)
 * 2. mix-in a proper [[bbuzz.TweetProvider]]
 * 3. provide all desired settings according to the `TweetProvider`
 * 4. implement the methods from [[bbuzz.TweetConsumer]]
 *
 * Example, will scan an Elasticsearch index and print all found tweets.
 *
 * {{{
 * object TweetPrinter extends TweetStreaming with ElasticsearchScanTweets {
 *   def host = "localhost"
 *   def port = 9200
 *   def index = "tweets"
 *
 *   def onTweet(tweet: Tweet) = println(tweet.getText)
 *   def handleException(exception: Throwable) = exception.printStackTrace()
 * }
 * }}}
 *
 * This object's main method can be used to run the app.
 */
abstract class TweetStreaming extends TweetConsumer {
  this: TweetProvider =>

  final def main(args: Array[String]) {
    val ts = tweets
    ts.subscribe(this, ComputationScheduler())
    ts.toBlockingObservable.foreach(_ => ())
  }
}
