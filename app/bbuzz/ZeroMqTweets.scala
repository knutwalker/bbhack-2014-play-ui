package bbuzz

import org.zeromq.ZMQ
import org.zeromq.ZMQ.Socket
import rx.lang.scala.Subject
import scala.annotation.tailrec
import scala.util.control.Exception.handling

/**
 * Provides tweets by listening on a ZeroMQ SUB socket.
 *
 * ZMQ message are picked from a [[http://api.zeromq.org/3-2:zmq-socket#toc10 ZMQ_SUB]] socket.
 * These messages can consist of multiple parts, where the first part is the channel name and
 * the second part is the actual message.
 *
 * The connection is always made over tcp.
 */
trait ZeroMqTweets extends TweetProvider {

  /**
   * The hostname of the publisher socket.
   * They should implement a [[http://api.zeromq.org/3-2:zmq-socket#toc9 ZMQ_PUB]] socket.
   *
   * If you connect too localhost, do not use `localhost` but `127.0.0.1` instead.
   */
  def host: String

  /**
   * The port of the publisher socket.
   * They should implement a [[http://api.zeromq.org/3-2:zmq-socket#toc9 ZMQ_PUB]] socket.
   */
  def port: Int

  /**
   * The ZMQ channel to subscribe to.
   */
  def channel: String


  private final lazy val zContext = ZMQ.context(1)
  private final lazy val socket: Socket = {
    val socketUrl = s"tcp://$host:$port"
    val s = zContext.socket(ZMQ.SUB)
    s.connect(socketUrl)
    s.subscribe(channel.getBytes(ZMQ.CHARSET))
    s
  }

  /**
   * Listens to publishes message and publishes them as [[bbuzz.Tweet]]s.
   * As calls to `ZMQ`s recv method on sockets can block, this will do so in a new Thread.
   *
   * @return an `Observable` of `Tweets`
   */
  final def tweets: TweetObservable = {
    val p = Subject[Tweet]()

    new Thread(new Runnable {

      private final val ch = channel

      @tailrec
      def recv(): String = {
        val t = socket.recvStr().trim
        if (t == ch) recv()
        else t
      }

      def run() = try {
        doRun()
      } catch {
        case t: Throwable => p.onError(t)
      }

      def doRun() = while (true) {
        val tweet = recv()
        val tweet1 = tryCreateTweet(tweet)
        tweet1.foreach(p.onNext)
      }
    }).start()

    p
  }
}
