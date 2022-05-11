import GrpcClientStub.JacksonClientStub
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannelBuilder
import qu.protocol.Shared.{QuorumSystemThresholds, ServerInfo}

import scala.concurrent.Future

//import that declares specific dependency
import qu.protocol.model.ConcreteQuModel._

//most abstract possible (not bound to grpc)
trait QuClient[U, Marshallable[_]] {
  def submit[T](op: Operation[T, U])(implicit
                                     marshallableRequest: Marshallable[Request[T, U]],
                                     marshallableResponse: Marshallable[Response[Option[T]]],
                                     marshallableRequestObj: Marshallable[Request[Object, U]],
                                     marshallableResponseObj: Marshallable[Response[Option[Object]]]): Future[T]
}

object QuClient {
  //todo should be put in the co of the class of the object that it creates (QuClientImpl) if wanting to forbid
  // creating QuClientImpl without this builder...
  trait AuthenticatedClientBuilderD[U]

  case class AuthenticatedClientBuilder[U, Marshallable[_]](private val serversInfo: Set[ServerInfo],
                                                            private val thresholds: Option[QuorumSystemThresholds],
                                                            private val policyFactory: (Set[ServerInfo], QuorumSystemThresholds) => QuorumPolicy[U, Marshallable]
                                                           ) extends AuthenticatedClientBuilderD[U] {
    //i have to pass to quClient: policy (server stubs)
    def addServer(serverInfo: ServerInfo): AuthenticatedClientBuilder[U, Marshallable] = {
      this.copy(serversInfo = serversInfo + serverInfo)
      //could create channels here instead
    }

    def withThresholds(thresholds: QuorumSystemThresholds): AuthenticatedClientBuilder[U, Marshallable] = {
      this.copy(thresholds = Some(thresholds))
    }

    def build = new QuClientImpl[U, Marshallable](policyFactory(serversInfo, thresholds.get), thresholds.get)
  }

  //should return a trait type
  def defaultBuilder[U](): AuthenticatedClientBuilderD[U] = simpleJacksonQuorumPolicyBuilder[U]()

  //specific builder instance-related utility
  private object AuthenticatedClientBuilder {
    def empty[U, Marshallable[_]](policyFactory: (Set[ServerInfo],
      QuorumSystemThresholds) => QuorumPolicy[U, Marshallable]) =
      AuthenticatedClientBuilder(Set(), Option.empty, (mySet, thresholds) => policyFactory(mySet, thresholds))
  }

  //implementations
  private def simpleJacksonQuorumPolicyBuilder[U](): AuthenticatedClientBuilderD[U] =
    AuthenticatedClientBuilder.empty[U, JavaTypeable]((mySet, thresholds) =>
      new SimpleBroadcastPolicy(thresholds,
        mySet.map(serverInfo => serverInfo.ip -> new JacksonClientStub(
          ManagedChannelBuilder.forAddress(serverInfo.ip,
            serverInfo.port)
            .build))
          .toMap))
}
