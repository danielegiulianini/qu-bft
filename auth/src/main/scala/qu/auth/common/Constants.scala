package qu.auth.common

import io.grpc.{Context, Metadata}

/**
 * Contains some authentication/authorization-related constants to be used over grpc.
 */
object Constants {
  /**
   * Secret private key shared between authorization server and resource server used to sign JWT token.
   */
  val JWT_SIGNING_KEY = "L8hHXsaQOUjk5rg7XPGv4eL36anlCrkMz8CJ0i/8E/0="
  val BEARER_TYPE = "Bearer"
  /**
   * Metadata key for referencing JWT token stored in the header for client requests to resource server.
   */
  val AUTHORIZATION_METADATA_KEY: Metadata.Key[String] = Metadata.Key.of("Authorization",
    Metadata.ASCII_STRING_MARSHALLER)
  /**
   * Key for referencing Client Id stored in the header for client requests to resource server.
   */
  val CLIENT_ID_CONTEXT_KEY: Context.Key[String] = Context.key("clientId")
}
