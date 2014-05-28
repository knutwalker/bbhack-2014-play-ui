package bbuzz

import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler
import scala.collection.immutable.Iterable

/**
 * Provides Tweets.
 *
 * You should create a trait or a class, implementing this Trait.
 * The only method to provide is `tweets`, that must return an [[rx.lang.scala.Observable]] of `[[bbuzz.Tweet]]s`.
 */
trait TweetProvider {

  /**
   * Tweets are provided as an [[rx.lang.scala.Observable]].
   *
   * You can implement this in any way you like, though you might want to have
   * a look at the companions object's helper traits.
   *
   * This `Observable` will be consumed by a [[bbuzz.TweetConsumer]]
   *
   * @return an `Observable` of `Tweets`
   */
  def tweets: TweetObservable
}

/**
 * Factory methods for [[bbuzz.TweetProvider]]
 */
object TweetProvider {

  /**
   * Created a `TweetProvider` from an existing [[bbuzz.TweetObservable]]
   *
   * @param os the Observable
   * @return a TweetProvider from the given Observable
   */
  def apply(os: TweetObservable): TweetProvider = new TweetProvider {
    def tweets: TweetObservable = os
  }

  /**
   * Create a `TweetProvider` from an [[scala.collection.immutable.Iterable]] of `String`s
   * where every item of the Iterable is a Tweet JSON.
   *
   * @param it the string iterable
   * @return a TweetProvider from the given string iterable
   */
  def apply(it: Iterable[String]): TweetProvider = new FromStringIterable {
    def iterable: Iterable[String] = it
  }

  /**
   * Helper trait to create a TweetProvider from a [[scala.collection.immutable.Iterable]] of `String`s.
   */
  trait FromStringIterable extends TweetProvider {

    /**
     * @return an Iterable of Strings, where every String is the JSON representation on a [[bbuzz.Tweet]]
     */
    def iterable: Iterable[String]

    final def tweets: TweetObservable = {
      val it = iterable
      Observable.from(it, IOScheduler()).
        map(createTweet).
        flatMap(s => Observable.from(s))
    }
  }
}
