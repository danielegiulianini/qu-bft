package qu.service

import io.grpc._
import io.jsonwebtoken.Jwts
import qu.QuServiceDescriptors
import qu.auth.common.Constants

import java.util.Objects

/**
 * A [[io.grpc.ServerInterceptor]] responsible for JWT-based authentication verification.
 */
class JwtAuthorizationServerInterceptor extends ServerInterceptor {

  private val parser = Jwts.parser.setSigningKey(Constants.JWT_SIGNING_KEY)

  def interceptCall[ReqT, RespT](serverCall: ServerCall[ReqT, RespT],
                                 metadata: Metadata,
                                 serverCallHandler: ServerCallHandler[ReqT, RespT]): ServerCall.Listener[ReqT] = {

    //disabling server-to-server authentication
    if (Objects.equals(serverCall.getMethodDescriptor.getServiceName, QuServiceDescriptors.OBJECT_REQUEST_METHOD_NAME))
      return serverCallHandler.startCall(serverCall, metadata)

    val value = metadata.get(Constants.AUTHORIZATION_METADATA_KEY)
    var status: Status = null
    if (value == null) status = Status.UNAUTHENTICATED.withDescription("Authorization token is missing")
    else if (!value.startsWith(Constants.BEARER_TYPE)) status = Status.UNAUTHENTICATED.withDescription("Unknown authorization type")
    else try {
      val token = value.substring(Constants.BEARER_TYPE.length).trim
      val claims = parser.parseClaimsJws(token)
      //the CLIENT_ID_CONTEXT_KEY is the one used by QuServiceImpl
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