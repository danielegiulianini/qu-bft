

//analogous of ClientSpec (still don't know if using junit or scalatest)
class ClientTests {

/*
mocking server for verifying client
import io.grpc.stub.StreamObserver
   @Test  def greet_messageDeliveredToServer(): Unit =  {
   val requestCaptor: Nothing = ArgumentCaptor.forClass(classOf[Nothing])
client.greet("test name")
verify(serviceImpl).sayHello(requestCaptor.capture, ArgumentMatchers.any[StreamObserver[Nothing]])
assertEquals("test name", requestCaptor.getValue.getName)
}
 */

  //...al primo giro invia la ohs vuota


  //***BASIC FUNCTIONING***
  //client returns if order >= q (and with...)

  //client performs a barrier (object operation) if order < q

  //client (mocko la quorumPolicy) contianua ad interrogare se l'ordine è minore di x

  //client backs off if contention




  //***ADVANCED FUNCTIONING***
  //scenari di scambi più lunghi ... vedere se rispetta anche andando avanti ...


}
