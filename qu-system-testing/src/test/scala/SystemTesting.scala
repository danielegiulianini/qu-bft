import org.scalatest.funspec.{AnyFunSpec, AsyncFunSpec}
import org.scalatest.matchers.should.Matchers
import qu.ServersFixture
import qu.client.{AuthenticatingClient, QuClient}
import qu.model.examples.Commands.{GetObj, Increment}
import qu.model.examples.OHSFixture

import scala.concurrent.Future

class SystemTesting extends AsyncFunSpec with Matchers
  with HealthyClusterFixture with AuthServerFixture with ServersFixture with OHSFixture {

  //type information survives network transit
  describe("A Q/U protocol interaction") {
    describe("when a query is issued") {
      it("should return to client the expected answer value") {

        //todo maybe to move to fixture (or maybe all the clientFuture?) (to be shutdown  correctly)
        val client = AuthenticatingClient[Int](authServerInfo.ip,
          authServerInfo.port,
          "username",
          "password")
        for {
          quClient <- for {
            _ <- client.register()
            builder <- client.authorize()
          } yield builder
            .addServers(quServerIpPorts)
            .withThresholds(thresholds).build
          value <- quClient.submit[Int](GetObj())
        } yield value should be(InitialObject)
        /*
         for {
           quClient <- for {
             _ <- client.register()
             builder <- client.authorize()
           } yield builder
             .addServers(quServerIpPorts)
             .withThresholds(thresholds).build
           _ <- quClient.submit[Unit](Increment())
           value <- quClient.submit[Int](GetObj())
         } yield value should be(Future.successful(InitialObject + 1))
*/
      }
    }
    describe("when an update is issued") {
      it("should return to client the expected answer value") {

        //todo maybe to move to fixture (or maybe all the clientFuture?) (to be shutdown  correctly)
        val client = AuthenticatingClient[Int](authServerInfo.ip,
          authServerInfo.port,
          "username",
          "password")
        for {
          quClient <- for {
            _ <- client.register()
            builder <- client.authorize()
          } yield builder
            .addServers(quServerIpPorts)
            .withThresholds(thresholds).build
          value <- quClient.submit[Unit](Increment())
        } yield value should be(()) //already out of future (no need for Future.successful)
        /*
         for {
           quClient <- for {
             _ <- client.register()
             builder <- client.authorize()
           } yield builder
             .addServers(quServerIpPorts)
             .withThresholds(thresholds).build
           _ <- quClient.submit[Unit](Increment())
           value <- quClient.submit[Int](GetObj())
         } yield value should be(Future.successful(InitialObject + 1))
*/
      }
    }
    /*describe("when an update is issued followed by a query") {
      it("should return to client the updated value") {

        //todo maybe to move to fixture (or maybe all the clientFuture?) (to be shutdown  correctly)
        val client = AuthenticatingClient[Int](authServerInfo.ip,
          authServerInfo.port,
          "username",
          "password")
        for {
          quClient <- for {
            _ <- client.register()
            builder <- client.authorize()
          } yield builder
            .addServers(quServerIpPorts)
            .withThresholds(thresholds).build
          _ <- quClient.submit[Unit](Increment())
          value <- quClient.submit[Int](GetObj())
        } yield value should be(Future.successful(InitialObject + 1))
        /*
         for {
           quClient <- for {
             _ <- client.register()
             builder <- client.authorize()
           } yield builder
             .addServers(quServerIpPorts)
             .withThresholds(thresholds).build
           _ <- quClient.submit[Unit](Increment())
           value <- quClient.submit[Int](GetObj())
         } yield value should be(Future.successful(InitialObject + 1))
*/
      }
    }*/

    //List.fill(3)(Increment()) :+ GetObj

  }
}

