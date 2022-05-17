package client

import client.QuorumPolicy.simpleJacksonPolicyFactoryUnencrypted
import com.fasterxml.jackson.module.scala.JavaTypeable

import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.model.ConcreteQuModel._

//most abstract possible (not bound to grpc)
trait QuClient[U, Transferable[_]] {
  def submit[T](op: Operation[T, U])(implicit
                                     marshallableRequest: Transferable[Request[T, U]],
                                     marshallableResponse: Transferable[Response[Option[T]]],
                                     marshallableRequestObj: Transferable[Request[Object, U]],
                                     marshallableResponseObj: Transferable[Response[Option[Object]]]): Future[T]
}

object QuClient {

  //could use builder factory (apply) instead of defaultBuilder
  def defaultBuilder[U](token: String, serversInfo: Set[ServerInfo], thresholds: QuorumSystemThresholds):
  AuthenticatedClientBuilderD[U] =
    simpleJacksonQuClientBuilderWithNamedParameter[U](token, serversInfo, thresholds)

  //todo complete methods (abstraction to hide Marshallable type param) but I think it is not possible...
  trait AuthenticatedClientBuilderD[U] {
    //def build() : client.QuClient[U, ]
  }

  //another style of building objects
  case class AuthenticatedClientBuilderWithNamedParam[U, Transportable[_]]( //programmer dependencies
                                                                            private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Transportable],
                                                                            //user dependencies
                                                                            private val serversInfo: Set[ServerInfo],
                                                                            private val thresholds: QuorumSystemThresholds,
                                                                            private val token: String
                                                                          ) extends AuthenticatedClientBuilderD[U] {
    def build: AuthenticatedQuClientImpl[U, Transportable] = {
      //todo missing validation
      new AuthenticatedQuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds), thresholds)
    }
  }

  //hided builder implementations
  def simpleJacksonQuClientBuilderWithNamedParameter[U](token: String, serversInfo: Set[ServerInfo], thresholds: QuorumSystemThresholds): AuthenticatedClientBuilderD[U] =
    AuthenticatedClientBuilderWithNamedParam[U, JavaTypeable](simpleJacksonPolicyFactoryUnencrypted(token), serversInfo, thresholds, token)
}








/*
* builder with functional style (still interesting):
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