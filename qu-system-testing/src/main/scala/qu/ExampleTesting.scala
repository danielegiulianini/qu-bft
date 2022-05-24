package qu

import com.fasterxml.jackson.module.scala.JavaTypeable

//showcasing a stub
object ExampleTesting extends App {
  //val a: Server = ServerBuilder.forPort(1).build()
  //ManagedChannelBuilder
  //Grpc.newChannelBuilder(
  /*
    class Fetch extends Operation[String, Int] {
      override def compute(obj: Int): String = "hello"
    }

    val myService = new QuService //builder mio
    myService.addOperation[String]()

    val server = InProcessServerBuilder
    //here you specify the executor
      .forName("cc")
      .addService(myService)
      .build
    server.start
    Thread.sleep(1000)

    val chan = InProcessChannelBuilder.forName("cc").build //ManagedChannelBuilder
    val stb = new JacksonClientStub[Int](chan)

    val myFuture = stb.send(Request(new Fetch, null))
    myFuture.onComplete({
      case Success(_) => println("future successed!")
      case Failure(exception) =>
        println(s"futures failed!, ${exception.getMessage}")
    })

    server.shutdown()

    import java.util.concurrent.TimeUnit

    server.awaitTermination(5, TimeUnit.SECONDS)*/

  def ola[T: JavaTypeable](a: T) = {
    println("ciao")
  }

  class Bello[Y] {
    def compute(x: Y) = x
  }

  ola(new Bello)

  class Wrapping[T](a: T)

  def metodoCheRichiedeImplicitDelWrappante[T](a: T)(implicit ab: JavaTypeable[Wrapping[T]]): Unit = {
  }

  //metodo che riduce impliciti
  def metodoCheRichiedeImplicitiSoloDelWrappato[T: JavaTypeable](a: T): Unit = {
    metodoCheRichiedeImplicitDelWrappante(new Wrapping(a))
  } // import com.roundeights.hasher.Digest.digest2string


  // println("l'hmac e':"+ "".hmac("ciao").md5)
  val t = Set(1, 2, 3)
  val myMap = t.map(id => id -> "ciao").toMap
  val myMap2 = t.map(_ -> "ciao").toMap

  println(myMap)

  //closure analysis in scala
  class StubClient {
    def ciao(a: () => Unit) = a()
  }

  val t2 = new StubClient()
  t2.ciao(() => println(t2))

  class MyClass {
    var a: Int = _
    var stubCl: StubClient = _

    def myMeth() = stubCl
  }

  println("cosa contiene l'bj di MyClass?: " + new MyClass().myMeth())
}
