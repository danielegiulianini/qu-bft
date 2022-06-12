package qu

import io.grpc.{CallCredentials, CallOptions, ManagedChannel, Metadata, Status}
import qu.auth.Token
import qu.auth.common.Constants

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext

abstract class JwtAsyncGrpcClientStub[Transferable[_]](override val chan: ManagedChannel, val token: Token)(implicit executor: ExecutionContext)
  extends AsyncGrpcClientStub[Transferable](chan) {
  override val callOptions = CallOptions.DEFAULT.withCallCredentials(new AuthenticationCallCredentials(token))
}

class AuthenticationCallCredentials(var token: Token) extends CallCredentials {
  override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
                                    executor: Executor,
                                    metadataApplier: CallCredentials.MetadataApplier): Unit = {
    executor.execute(() => {
      def fillHeadersWithToken() = {
        try {
          val headers = new Metadata
          headers.put(Constants.AUTHORIZATION_METADATA_KEY, String.format("%s %s", Constants.BEARER_TYPE, token.username))
          metadataApplier.apply(headers)
        } catch {
          case e: Throwable =>
            metadataApplier.fail(Status.UNAUTHENTICATED.withCause(e))
        }
      }

      fillHeadersWithToken()
    })
  }

  override def thisUsesUnstableApi(): Unit = {}
}
