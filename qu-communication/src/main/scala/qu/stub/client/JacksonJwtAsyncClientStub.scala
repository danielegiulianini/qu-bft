package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannel
import presentation.CachingMethodDescriptorFactory
import qu.JacksonMethodDescriptorFactory
import qu.auth.Token

import scala.concurrent.ExecutionContext

/**
 * An implementation of [[qu.stub.client.AsyncClientStub]] authenticated by means of a JWT-token and leveraging
 * Jackson (so, JSON) as (de)serialization technology.
 */
class JacksonJwtAsyncClientStub(channel: ManagedChannel, token: Token)(implicit executor: ExecutionContext)
  extends JwtAsyncClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory
    with CachingMethodDescriptorFactory[JavaTypeable]