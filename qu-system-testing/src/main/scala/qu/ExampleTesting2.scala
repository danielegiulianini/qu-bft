package qu

import io.grpc.inprocess.InProcessServerBuilder
import qu.ProvaSubtype.Increment
import qu.StubFactories.inNamedProcessJacksonStubFactory
import qu.model.ConcreteQuModel.{Query, Request, Response, emptyAuthenticatedRh, emptyOhs, emptyRh, nullAuthenticator}
import qu.model.QuorumSystemThresholds
import qu.service.AbstractQuService.{ServerInfo, jacksonSimpleQuorumServiceFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global

object ExampleTesting2 extends App {
  println("try to send to server with a jacksonclientStub...")

  val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()


  val serverInfo = ServerInfo(ip =  "ciao", port = 2, keySharedWithMe = "hmackey")
  def id(serverInfo: ServerInfo): String = serverInfo.ip + ":" + serverInfo.port


  val service = serviceFactory(serverInfo, 8, QuorumSystemThresholds(1, 2, 3, 4, 5))


  class MyQuery extends Query[Int, Int] {
    override def whatToReturn(obj: Int): Int = obj
  }

  service.addOperationOutput[Int]()


  //simulare una InprocessQuServer
  val server = InProcessServerBuilder
    .forName(id(serverInfo)) //ip + ":" + port)
    //.intercept(new AuthorizationServerInterceptor())
    .addService(service)
    .build

  //must register operations!

  server.start()

  Thread.sleep(1000)


  //jacksonclientstub con canale inprocess
  val myStub = inNamedProcessJacksonStubFactory(serverInfo.ip, serverInfo.port)
  val fut = myStub.send[Request[Int, Int], Response[Option[Int]]](//  send[Request[Unit, Int], Response[Option[Int]]]
    Request(operation = Some(new MyQuery()),//Option.empty[Query[Int, Int]],
      ohs = emptyOhs(Set(id(serverInfo)))))
println("le ohs empty : " + emptyOhs(Set(id(serverInfo))))
println("la eptyauthrh: " + emptyAuthenticatedRh)
  println("la eptyrh: " + emptyRh)
  println("la nullauthent: " + nullAuthenticator)

  println("---una tupla con una mappa vuota: " + (emptyRh, nullAuthenticator))
  println("la fut :" + fut)
  fut.onComplete(res => println("al completamento: " + res))



  Thread.sleep(4000)
  println("closing...")

  myStub.shutdown()
  server.shutdown()
  server.awaitTermination(5, TimeUnit.SECONDS)
}
