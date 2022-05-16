import GrpcClientStub.UnauthenticatedJacksonClientStub
import QuorumPolicy.{PolicyFactory, simpleJacksonPolicyFactoryUnencrypted}
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannelBuilder
import Shared.{QuorumSystemThresholds, RecipientInfo => ServerInfo}

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

  //should return a trait type
  //could use builder factory instead of defaultBuilder
  def defaultBuilder[U](token: String): AuthenticatedClientBuilderD[U] = simpleJacksonQuClientBuilderInFunctionalStyle[U](token)

  //todo complete methods (abstraction to hide Marshallable type param) but I think it is not possible...
  trait AuthenticatedClientBuilderD[U] {
    //def build() : QuClient[U, ]
  }

  //another style of building objects
  case class AuthenticatedClientBuilderWithNamedParam[U, Transportable[_]]( //programmer dependencies
                                                                            private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Transportable],
                                                                            //user dependencies
                                                                            private val serversInfo: Set[ServerInfo],
                                                                            private val thresholds: QuorumSystemThresholds,
                                                                            private val token: String
                                                                          ) extends AuthenticatedClientBuilderD[U] {
    def build: QuClientImpl[U, Transportable] = {
      //todo missing validation
      new QuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds), thresholds)
    }
  }

  case class AuthenticatedClientBuilderInFunctionalStyle[U, Transportable[_]]( //programmer dependencies
                                                                               private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Transportable],
                                                                               //user dependencies
                                                                               private val serversInfo: Set[ServerInfo],
                                                                               private val thresholds: Option[QuorumSystemThresholds],
                                                                               private val token: String
                                                                             ) extends AuthenticatedClientBuilderD[U] {

    def addServer(serverInfo: ServerInfo): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
      this.copy(serversInfo = serversInfo + serverInfo) //could create channels here instead of creating in policy

    def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilderInFunctionalStyle[U, Transportable] =
      this.copy(thresholds = Some(thresholds))

    def build: QuClientImpl[U, Transportable] = {
      //todo missing validation
      new QuClientImpl[U, Transportable](policyFactory(serversInfo, thresholds.get), thresholds.get)
    }
  }

  //builder companion object specific builder instance-related utility
  private object AuthenticatedClientBuilderInFunctionalStyle {
    def empty[U, Transferable[_]](policyFactory: PolicyFactory[Transferable, U], token: String): AuthenticatedClientBuilderInFunctionalStyle[U, Transferable] =
      AuthenticatedClientBuilderInFunctionalStyle((mySet, thresholds) => policyFactory(mySet, thresholds), Set(), Option.empty, token)
  }

  //hided builder implementations
  private def simpleJacksonQuClientBuilderInFunctionalStyle[U](token: String): AuthenticatedClientBuilderD[U] =
    AuthenticatedClientBuilderInFunctionalStyle.empty[U, JavaTypeable](simpleJacksonPolicyFactoryUnencrypted(token), token)

}
