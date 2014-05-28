package bbuzz


trait Hashings {
  import scala.util.hashing.MurmurHash3

  type Hashing = String => Int

  private val h1: Hashing = MurmurHash3.stringHash
  private val h2: Hashing = s => {
    val a = 7
    s.foldLeft(0) { (h, char) =>
      (a * char) + (h << 6) + (h << 16) - h
    }
  }

  private def g(i: Int): Hashing = s => h1(s) + i * h2(s)

  def h(p: Int, i: Int): Hashing = g(i) andThen (math.abs(_) % p)
}


trait CMSketch {
  this: Hashings =>

  def bucketSize: Int
  def hashSize: Int


  private lazy val table = Array.fill(hashSize, bucketSize)(0)

  def sketchCount(s: String): Unit = (0 until hashSize).foreach { n =>
    val bucket = h(bucketSize, n)(s)
    table(n)(bucket) += 1
  }

  def sketchGet(s: String): Int = (0 until hashSize).foldLeft(-1) { (sum, n) =>
    val bucket = h(bucketSize, n)(s)
    val bucketVal = table(n)(bucket)
    if (sum == -1) bucketVal else sum min bucketVal
  }
}

trait HeavyHitters {
  implicit val topIntOrd = implicitly[Ordering[Int]].reverse

  def maxK: Int

  var items = Map.empty[String, Int]

  private def update(s: String, c: Int) = items += (s -> c)

  private def addNew(s: String) = update(s, 1)

  private def replace(s: String) = {
    val sorted = items.toVector.sortBy(_._2)
    items = (sorted.init :+ (s -> (sorted.last._2 + 1))).toMap
  }

  def insert(s: String) = items.get(s) match {
    case Some(c)                    => update(s, c + 1)
    case None if items.size < maxK  => addNew(s)
    case None                       => replace(s)
  }

  def lookup(s: String) = items.getOrElse(s, 0)

  def topK() = items.toList.sortBy(_._2)
}
