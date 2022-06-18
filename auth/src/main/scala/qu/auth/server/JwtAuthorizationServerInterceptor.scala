package qu.auth.server

import io.grpc._
import io.jsonwebtoken.Jwts
import qu.auth.common.Constants

class JwtAuthorizationServerInterceptor extends ServerInterceptor {
  private val parser = Jwts.parser.setSigningKey(Constants.JWT_SIGNING_KEY)

  def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT],
                                 metadata: Metadata,
                                 serverCallHandler: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {
    val value = metadata.get(Constants.AUTHORIZATION_METADATA_KEY)
    var status: Status = null
    if (value == null) status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing")
    else if (!value.startsWith(Constants.BEARER_TYPE)) status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type")
    else try {
      val token = value.substring(Constants.BEARER_TYPE.length).trim
      val claims = parser.parseClaimsJws(token)
      //the CLIENT_ID_CONTEXT_KEY is the one used by QuServerImpl
      val ctx = Context.current.withValue(Constants.CLIENT_ID_CONTEXT_KEY, claims.getBody.getSubject)
      return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler)
    } catch {
      case e: Exception =>
        status = Status.UNAUTHENTICATED.withDescription(e.getMessage).withCause(e)
    }
    serverCall.close(status, metadata)
    new ServerCall.Listener[ReqT]() {
    }
  }
}