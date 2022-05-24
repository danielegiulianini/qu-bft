package qu.model

case class QuorumSystemThresholds(t: Int, q: Int, b: Int, r: Int) {
  //validation
}

object QuorumSystemThresholds {
  //factory for only 3 param
  def apply(t: Int, q: Int, b: Int): QuorumSystemThresholds = QuorumSystemThresholds(t, q, b, t + b + 1)
}

object Prova {
  val j: QuorumSystemThresholds = QuorumSystemThresholds(1, 2, 3)
}