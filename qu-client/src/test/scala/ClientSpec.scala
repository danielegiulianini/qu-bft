import com.fasterxml.jackson.module.scala.JavaTypeable
import org.scalamock.context.MockContext
import org.scalamock.scalatest.MockFactory
import org.scalamock.util.MacroAdapter.Context
import org.scalatest.funspec.AnyFunSpec
import qu.Shutdownable
import qu.client.{AuthenticatedClientBuilderInFunctionalStyle, AuthenticatedQuClientImpl, BackOffPolicy, ClientQuorumPolicy}
import qu.model.QuorumSystemThresholds

//analogous of ClientSpec (still don't know if using junit or scalatest)
class ClientTests  extends AnyFunSpec with MockFactory //extends OHSFixture{
{
  //no builder since I want to plug
  //val clientUnderTest = AuthenticatedQuClientImpl()
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

  //c'è la questione delle fasi'
  //devo tirar su tanti server... no basta una quorum policy
  //devo avere un altra stub per simulare contentnion

  //non ho tante leve … ho
  // 1. la answer ritornata da ispezionare
  // le invocazioni a quorumrpc (numero e argomenti (captor) operation null)
  // l'invocazioni a backoff (solo numero)


  //...al primo giro invia la ohs vuota
  val quorumPolicy= mock[ClientQuorumPolicy[Int, JavaTypeable] with Shutdownable]
  val backOffPolicy = mock[BackOffPolicy]

  case class Ciao (t : ClientQuorumPolicy[Int, JavaTypeable])
  Ciao(quorumPolicy)

  val client = new AuthenticatedQuClientImpl[Int, JavaTypeable](policy = quorumPolicy,
    backoffPolicy = backOffPolicy,
    serversIds = Set(),
    thresholds = QuorumSystemThresholds(1, 2, 3))

  //***BASIC FUNCTIONING***
  //client returns if order >= q (and with...)

  //client performs a barrier (object operation) if order < q

  //client (mocko la quorumPolicy) contianua ad interrogare se l'ordine è minore di x

  //client backs off if contention


  //***ADVANCED FUNCTIONING***
  //scenari di scambi più lunghi ... vedere se rispetta anche andando avanti ...


}
