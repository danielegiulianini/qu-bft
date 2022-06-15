package qu.view.console

object StringUtils {
  def padRight(s: String, n: Int): String = {
    String.format("%-" + n + "s", s);
  }

  def padRight2(s: String, n: Int): String = {
    s.padTo(n, ' ')
  }

  def concatenateByNewLine(s: Iterable[String]) = s.mkString("\n")

  def concatenateByNewLine(s: String*) = s.mkString("\n")

}
