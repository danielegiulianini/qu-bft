package qu.stub.client

import io.grpc._
import qu.auth.Token
import qu.auth.common.Constants

import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext


/**
 * An implementation of [[qu.stub.client.AsyncClientStub]] authenticated by means of a JWT-token.
 */
abstract class JwtAsyncClientStub[Transferable[_]](override val chan: ManagedChannel, val token: Token)
                                                  (implicit executor: ExecutionContext)
  extends AbstractAsyncClientStub[Transferable](chan) {
  override val callOptions: CallOptions = CallOptions.DEFAULT.withCallCredentials(new AuthenticationCallCredentials(token))
    .withWaitForReady()
}

class AuthenticationCallCredentials(var token: Token) extends CallCredentials {
  override def applyRequestMetadata(requestInfo: CallCredentials.RequestInfo,
                                    executor: Executor,
                                    metadataApplier: CallCredentials.MetadataApplier): Unit = {
    executor.execute(() => {
      def fillHeadersWithToken(): Unit = {
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
