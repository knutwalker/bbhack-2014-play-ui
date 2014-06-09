package bbuzz

abstract class BBHack(val maxK: Int, val bucketSize: Int, val hashSize: Int) extends TweetStreaming
with HeavyHitters with CMSketch with Hashings {
  this: TweetProvider =>

  val languages = Set("en", "de")

  def doACount(s: String) = {
    insert(s)
    sketchCount(s)
  }

  def handleException(exception: Throwable): Unit = exception.printStackTrace()

  def onTweet(tweet: bbuzz.Tweet): Unit = {
    if (languages(tweet.getLang))
      Some(tweet.getHashtagEntities.map(_.getText)).getOrElse(Array()).filterNot(_.toLowerCase == "rt").foreach(doACount)
  }
}
