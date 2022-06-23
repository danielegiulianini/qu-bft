package qu

import io.jsonwebtoken.{Jwts, SignatureAlgorithm}
import qu.RecipientInfo.id
import qu.auth.Token
import qu.auth.common.Constants
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.model.ConcreteQuModel.{Request, emptyOhs}
import qu.model.OHSUtilities
import qu.model.examples.Commands.GetObj
import qu.service.datastructures.RemoteCounterServer
import qu.service.{LocalQuServerCluster, ServersFixture}
import qu.stub.client.{JacksonAuthenticatedStubFactory, JacksonStubFactory}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object ProvaChannelShutdown extends App with ServersFixture with OHSUtilities {
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  var healthyCluster = LocalQuServerCluster[Int](quServerIpPorts,
    keysByServer,
    thresholds,
    RemoteCounterServer.builder,
    InitialObject)
  healthyCluster.start()

  //tiro su server
  private def getJwt: Token =
    Token(Jwts.builder.setSubject("clientId").signWith(SignatureAlgorithm.HS256, Constants.JWT_SIGNING_KEY).compact)

  //creo policy
  var pol = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
    quServerIpPorts
      .map { recipientInfo =>
        id(recipientInfo) -> new JacksonAuthenticatedStubFactory()
          .unencryptedDistributedJwtStub(getJwt, recipientInfo)
      }
      .toMap)
  //pol.quorum(Request)

  //shutodown
  healthyCluster.shutdown()
  println("is the clister soon shutdown? " + healthyCluster.isShutdown)
  while (healthyCluster.isShutdown != true) {
  }
  println("is the clister soon shutdown? " + healthyCluster.isShutdown)

  pol.shutdown()
  println("is the policy soon shutdown? " + pol.isShutdown)
  while (pol.isShutdown != true) {
  }
  println("ipolicy sut down!" + pol.isShutdown)

   /*healthyCluster = LocalQuServerCluster[Int](quServerIpPorts,
     keysByServer,
     thresholds,
     RemoteCounterServer.builder,
     InitialObject)
   healthyCluster.start()
   pol = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
     quServerIpPorts
       .map { recipientInfo =>
         id(recipientInfo) -> new JacksonAuthenticatedStubFactory()
           .unencryptedDistributedJwtStub(getJwt, recipientInfo)
       }
       .toMap)
   pol.quorum(Some(GetObj[Int]()), emptyOhs(serverIds))
   pol.shutdown()*/
}

