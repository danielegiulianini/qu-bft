package qu

import io.grpc.inprocess.InProcessServerBuilder
import qu.StubFactories.inNamedProcessJacksonStubFactory
import qu.model.ConcreteQuModel.Request
import qu.model.ConcreteQuModel.{Query, Request, Response}
import qu.service.AbstractQuService.jacksonSimpleQuorumServiceFactory

import java.util.concurrent.TimeUnit

object ExampleTesting2 extends App {
  /*println("try to send to server with a jacksonclientStub...")

  val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()

  val ip = "ciao"
  val port = 2  //val serviceInfo = RecipientInfo(ip = ip, port = port, keySharedWithMe = "hmackey")


  //ip port key wuorumthresholds obj quorumSystemThresholds, serverInfo, obj
  val service = serviceFactory(ip = "ip", 2, "key", null, 8)


  class MyQuery extends Query[Int, Int] {
    override def whatToReturn(obj: Int): Int = obj
  }

  service.addOp[Int]()


  //simulare una InprocessQuServer
  val server = InProcessServerBuilder
    .forName(ip + ":" + port) //.intercept(new AuthorizationServerInterceptor())
    .addService(service)
    .build

  //must register operations!

  server.start()

  Thread.sleep(1000)


  //jacksonclientstub con canale inprocess
  val myStub = inNamedProcessJacksonStubFactory(serviceInfo)

  myStub.send2(
    Request(operation = Option.empty[Query[Int, String]],
      ohs = null))


  Thread.sleep(4000)
  println("closing...")

  server.shutdown()
  server.awaitTermination(5, TimeUnit.SECONDS)*/
}
