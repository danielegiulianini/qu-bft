package qu.model

case class QuorumSystemThresholds(t: Int, q: Int, b: Int, r: Int, n: Int) {
  //validation
}

object QuorumSystemThresholds {
  //factory for only 3 param (the other with default)
  def apply(t: Int, b: Int): QuorumSystemThresholds = QuorumSystemThresholds(t, 2 * t + 2 * b + 1, b, t + b + 1, getN(t = t, b = b))

  def apply(t: Int, q: Int, b: Int): QuorumSystemThresholds = QuorumSystemThresholds(t, q, b, t + b + 1, getN(t = t, b = b))

  private def getN(t: Int, b: Int): Int = 3 * t + 2 * b + 1
}

object Prova {
  val j: QuorumSystemThresholds = QuorumSystemThresholds(1, 2, 3)
}