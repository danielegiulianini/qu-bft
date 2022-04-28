import Implementations.jacksonService


trait QuServer {
  def start(): Unit

  def stop(): Unit
}

//companion object
object QuServer {
  def apply() : QuServer = null
}

//a facade that hides grpc internals
//here freedom... to split APIs?
class QuServerImpl {

}

//alternative to apply in companion object
class QuServerBuilder {

  //how I inject this dependency?? factory as parameter or actual service as parameter?
  val myService = jacksonService()

  def addOperation(): Unit = {
    myService.addOperation()
  }

  def addServer(): Unit = {

  }

  def build(): QuServer = QuServer()
}

