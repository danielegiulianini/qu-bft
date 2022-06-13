import io.grpc.{Server, ServerBuilder}
import io.grpc.inprocess.InProcessChannelBuilder
import qu.auth.client.AuthClient
import qu.auth.AuthGrpc
import qu.auth.server.MyAuthService

import scala.concurrent.ExecutionContext
import scala.util.Failure


//prova per vedere che ecceioni vengono solletvate prima di costruire il test
object AttemptException extends App {

  //start inprocess server...

  import io.grpc.inprocess.InProcessChannelBuilder
  import io.grpc.inprocess.InProcessServerBuilder

  val inprocessServerName = "greeting"

  /*val server = InProcessServerBuilder.forName(inprocessServerName)
    .addService(new MyAuthService()).build*/
  //val server = ServerBuilder.forPort(2).addService(GreeterGrpc.bindService(new GreeterImpl, executionContext)).build.start
  implicit val ec = ExecutionContext.global

  //todo pool lato server richiesto esplicitamente...
  val server = InProcessServerBuilder.forName(inprocessServerName)
    .addService(AuthGrpc.bindService(new MyAuthService, ec)).build.start

  println("hello from AttemptException ")
  //create client inprocesschannel

 // val channel = InProcessChannelBuilder.forName(inprocessServerName).usePlaintext.build

  //create client passing in process channel
  val authClient = AuthClient(inprocessServerName)
  val fut = authClient.authorize("ciao", "ciao")

  fut.onComplete(println(_))
  fut.onComplete{case Failure(f) => {
    println("la cause : "+ f.getCause)
    println("il msg : "+ f.getMessage)
  }}

  println("la fut e':" + fut)

  Thread.sleep(5000)

  import java.util.concurrent.TimeUnit

  //channel.shutdown.awaitTermination(1, TimeUnit.SECONDS)
  server.shutdown.awaitTermination

}
