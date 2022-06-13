package qu

import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import qu.client.quorum.JacksonSimpleBroadcastClientPolicy
import qu.stub.client.JwtAsyncClientStub

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

class SimpleClientQuorumPolicySpec extends AnyFunSpec with MockFactory with FourServersScenario {

  val mockedServersStubs = serversIds.map(_ -> mock[JwtAsyncClientStub[JavaTypeable]]).toMap

  //determinism in tests
  implicit val exec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val policy = new JacksonSimpleBroadcastClientPolicy[Int](thresholds,
    mockedServersStubs
  )

  //sends to all servers

  //if someone not responding (or responding with fail) resending to all

  //sending to already obtained responses

  //not counting 2 times the same server (il 1o server invia dopo un po' di tempo e poi ne invia due)
  //il server aspetta il ri-invio e poi ne spedisce due

  //votes count ok

  //ohs edited ok

  //if server responds always with fail with it's not included in success set

  //only single round if ohs is updated
}
