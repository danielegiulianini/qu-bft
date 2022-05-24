package qu

import io.grpc.inprocess.InProcessServerBuilder
import qu.StubFactories.{inNamedProcessJacksonStubFactory, inProcessJacksonJwtStubFactory}
import qu.auth.Constants
import qu.model.ConcreteQuModel.{Query, Request, Response}
import qu.service.AbstractQuService.jacksonSimpleQuorumServiceFactory
import qu.service.JwtAuthorizationServerInterceptor

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global

//prova con authentication jwt
object ExampleTesting3 extends App {
/*
  import io.jsonwebtoken.{Jwts, SignatureAlgorithm}

  private def getJwt: String = {
    Jwts.builder.setSubject("GreetingClient").signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact
  }

  println("try to send to server with a jacksonclientStub...")

  val serviceFactory = jacksonSimpleQuorumServiceFactory[Int]()

  val ip = "ciao"
  val port = 2
  //val serviceInfo = RecipientInfo(ip = ip, port = port, keySharedWithMe = "hmackey")


  //quorumSystemThresholds, serverInfo, obj
  val service = serviceFactory(null, token, ip, port, 8)


  class MyQuery extends Query[Int, Int] {
    override def whatToReturn(obj: Int): Int = obj
  }

  //must register operations!
  service.addOp[Int]()


  //simulare una InprocessQuServer
  val server = InProcessServerBuilder
    .forName(ip + ":" + port)
    .intercept(new JwtAuthorizationServerInterceptor())
    .addService(service)
    .build

  server.start()

  Thread.sleep(1000)


  //jacksonclientstub con canale inprocess
  //val myStub = inProcessJacksonStubFactory(serviceInfo)
  val myStub = inProcessJacksonJwtStubFactory(getJwt, token, ip, port)

  val fut = myStub.send2[Request[Int, Int], Response[Option[Int]]](
    Request(operation = Option.empty[Query[Int, Int]],
      ohs = null))

  fut.onComplete(println(_))
  println("la fut e':" + fut)


  Thread.sleep(4000)
  println("closing...")

  server.shutdown()
  server.awaitTermination(5, TimeUnit.SECONDS)*/
}
