import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec

class Ciao {
  def ciao() = "ciao"
}

trait Salve[T]{
  def salve() : T
}

class Salve1 extends Salve[Unit]{
  override def salve(): Unit = "come va"
}

case class Ola(s:Unit, s2:Int)

class ExampleTest extends AnyFunSpec {
  println("ok" + new Ciao)
  Ola(3, 2)

  //thrsholds compatibili ( r minore di q)
}
