package qu.client

import ClientQuorumPolicy.simpleJacksonPolicyFactoryUnencrypted
import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.client.AuthenticatedClientBuilderInFunctionalStyle.simpleJacksonQuClientBuilderInFunctionalStyle
import qu.model.QuorumSystemThresholds

import scala.concurrent.Future

//import that declares specific dependency
import qu.model.ConcreteQuModel._

//most abstract possible (not bound to grpc)
trait QuClient[ObjectT, Transferable[_]] {
  def submit[ReturnValueT](op: Operation[ReturnValueT, ObjectT])(implicit
                                                                 marshallableRequest: Transferable[Request[ReturnValueT, ObjectT]],
                                                                 marshallableResponse: Transferable[Response[Option[ReturnValueT]]],
                                                                 marshallableRequestObj: Transferable[Request[Object, ObjectT]],
                                                                 marshallableResponseObj: Transferable[Response[Option[Object]]]): Future[ReturnValueT]

  def shutdown()
}

object QuClient {
  def defaultBuilder[U](token: String): AuthenticatedClientBuilderInFunctionalStyle[U, JavaTypeable] =
    simpleJacksonQuClientBuilderInFunctionalStyle[U](token)
}


/*3. Builder with only named parameters

   def apply[U](token: String, serversInfo: Set[ServerInfo], thresholds: QuorumSystemThresholds):
   QuClient[U, JavaTypeable] =
     simpleJacksonQuClient[U](token, serversInfo, thresholds)

   //private factories internal use only
   private def apply[U, Transportable[_]](
                                           policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => ClientQuorumPolicy[U, Transportable],
                                           //user dependencies,
                                           serversInfo: Set[ServerInfo],
                                           thresholds: QuorumSystemThresholds): QuClient[U, Transportable] = {
     //validation
     new AuthenticatedQuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds),
       serversInfo.map(serverInfo => serverInfo.ip + serverInfo.port), thresholds)
   }*/

//hided builder implementations
/*def simpleJacksonQuClient[U](token: String, serversInfo: Map[String, Int], thresholds: QuorumSystemThresholds):
QuClient[U, JavaTypeable] =
  QuClient(simpleJacksonPolicyFactoryUnencrypted(token), serversInfo, thresholds)
*/

/* 2. Builder with named param (validation logic has been moved from build method to apply
object QuClient {

  //could use builder factory (apply) instead of defaultBuilder
  def defaultBuilder[U](token: String, serversInfo: Set[ServerInfo], thresholds: QuorumSystemThresholds):
  AuthenticatedClientBuilder[U, JavaTypeable] =
  //AuthenticatedClientBuilderD[U] =
    simpleJacksonQuClientBuilder[U](token, serversInfo, thresholds)

  //todo complete methods (abstraction to hide Marshallable type param) but I think it is not possible...
 /* trait AuthenticatedClientBuilderD[U] {
    //def build() : client.QuClient[U, ]
  }*/

  //another style of building objects
  case class AuthenticatedClientBuilder[U, Transportable[_]]( //programmer dependencies
                                                              private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Transportable],
                                                              //user dependencies
                                                              private val serversInfo: Set[ServerInfo],
                                                              private val thresholds: QuorumSystemThresholds,
                                                              private val token: String
                                                                          ) { //extends AuthenticatedClientBuilderD[U] {
    def build(): AuthenticatedQuClientImpl[U, Transportable] = {
      //todo missing validation
      new AuthenticatedQuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds), thresholds)
    }
  }

  //hided builder implementations
  def simpleJacksonQuClientBuilder[U](token: String, serversInfo: Set[ServerInfo], thresholds: QuorumSystemThresholds): AuthenticatedClientBuilder[U, JavaTypeable] =
    AuthenticatedClientBuilder[U, JavaTypeable](simpleJacksonPolicyFactoryUnencrypted(token), serversInfo, thresholds, token)
}

 */
/*
* 1. builder with functional style (still interesting):
  /*case class AuthenticatedClientBuilderInFunctionalStyle[U, Transportable[_]]( //programmer dependencies
                                                                               private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => client.QuorumPolicy[U, Transportable],
                                                                               //user dependencies
                                                                               private val serversInfo: Set[ServerInfo],
                                                                               private val thresholds: Option[QuorumSystemThresholds],
                                                                             ) extends AuthenticatedClientBuilderD[U] {

    def addServer(serverInfo: ServerInfo): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
      this.copy(serversInfo = serversInfo + serverInfo) //could create channels here instead of creating in policy

    def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
      this.copy(thresholds = Some(thresholds))

    def build: client.AuthenticatedQuClientImpl[U, Transportable] = {
      //todo missing validation
      new client.AuthenticatedQuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds.get), thresholds.get)
    }
  }

  //builder companion object specific builder instance-related utility
  private object AuthenticatedClientBuilderInFunctionalStyle {
    def empty[U, Transferable[_]](policyFactory: PolicyFactory[Transferable, U], token: String): AuthenticatedClientBuilderInFunctionalStyle[U, Transferable] =
      AuthenticatedClientBuilderInFunctionalStyle((mySet, thresholds) => policyFactory(mySet, thresholds), Set(), Option.empty, token)
  }


  def defaultBuilder[U](token: String): AuthenticatedClientBuilderD[U] = simpleJacksonQuClientBuilderWithFunctionalStyle[U](token)

  //hided builder implementations
  private def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: String): AuthenticatedClientBuilderD[U] =
    AuthenticatedClientBuilderInFunctionalStyle.empty[U, JavaTypeable](simpleJacksonPolicyFactoryUnencrypted(token), token)
*/
* */
