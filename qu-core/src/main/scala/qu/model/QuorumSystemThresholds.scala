package qu.model

case class QuorumSystemThresholds(t: Int, q: Int, b: Int, r: Int, n: Int) {
  def validate(): Unit = {
    if (r < t + b + 1) throw new IllegalArgumentException("minimum threshold for r is > t + b")
    if (q < 2 * t + 2 * b + 1) throw new IllegalArgumentException("minimum threshold for q is > 2t + 2b")
    if (n < 3 * t + 2 * b + 1) throw new IllegalArgumentException("minimum threshold for n is > 3t + 2b")
  }

  validate()
}

object QuorumSystemThresholds {

  //public factory method for only 2 param (the other with default)
  def apply(t: Int, b: Int): QuorumSystemThresholds =
    QuorumSystemThresholds(t, getQFromTAndB(t, b), b, getRFromTAndB(t, b), getNFromTAndB(t = t, b = b))

  //factory method for only 3 param (the other with default)
  def apply(t: Int, q: Int, b: Int): QuorumSystemThresholds =
    QuorumSystemThresholds(t, q, b, t + b + 1, getNFromTAndB(t = t, b = b))

  private def getNFromTAndB(t: Int, b: Int): Int = 3 * t + 2 * b + 1

  private def getQFromTAndB(t: Int, b: Int): Int = 2 * t + 2 * b + 1

  private def getRFromTAndB(t: Int, b: Int): Int = t + b + 1

}
