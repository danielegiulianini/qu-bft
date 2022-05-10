//import Implementations.jacksonService
import qu.protocol.ConcreteQuModel._


trait QuServer {
  def start(): Unit

  def stop(): Unit
}

//companion object
object QuServer {
  def apply[U](quService: QuService[U]): QuServer = null
}

//a facade that hides grpc internals
//here freedom... to split APIs?
class QuServerImpl {

}

//alternative to apply in companion object
class QuServerBuilder[MyMarshallable[_], U](
                                             /*how do I inject this dependency?? factory as parameter or actual service as parameter or use inheritance (protected methods)?*/
                                             private val myService: QuServiceImpl[U, MyMarshallable]) {

  def addOperation[T]()(implicit 
                        marshallableRequest: MyMarshallable[Request[T, U]],
                        marshallableResponse: MyMarshallable[Response[Option[T]]],
                        marshallableLogicalTimestamp: MyMarshallable[LogicalTimestamp],
                        marshallableObjectSyncResponse: MyMarshallable[ObjectSyncResponse[U]]): Unit = {
    //myService.addOperation[T]()
  }

  def addServer(/*server description*/): Unit = {

  }

  def addCluster(): Unit = {

  }

  def build(): QuServer = ??? // QuServer(myService)
}

