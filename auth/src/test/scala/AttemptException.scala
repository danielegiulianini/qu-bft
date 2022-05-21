import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import qu.auth.{HelloWorldClient, MyAuthService}


//prova per vedere che ecceioni vengono solletvate prima di costruire il test
object AttemptException extends App {

  //start inprocess server...
  import io.grpc.inprocess.InProcessChannelBuilder
  import io.grpc.inprocess.InProcessServerBuilder

  val inprocessServerName = "greeting"

  val server = InProcessServerBuilder.forName(inprocessServerName)
    .addService(new MyAuthService()).build

  //create client inprocesschannel

  val channel = InProcessChannelBuilder.forName(inprocessServerName).usePlaintext.build



  //create client passing in process channel
  val authClient = HelloWorldClient(inprocessServerName)


//authClient.authorize()
  //authClient.register()


  import java.util.concurrent.TimeUnit
  channel.shutdown.awaitTermination(1, TimeUnit.SECONDS)
  server.shutdown.awaitTermination

}
