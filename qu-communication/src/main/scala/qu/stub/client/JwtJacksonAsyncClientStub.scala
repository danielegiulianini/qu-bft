package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannel
import presentation.CachingMethodDescriptorFactory
import qu.JacksonMethodDescriptorFactory
import qu.auth.Token

import scala.concurrent.ExecutionContext


class JwtJacksonAsyncClientStub(channel: ManagedChannel, token: Token)(implicit executor: ExecutionContext)
  extends JwtAsyncClientStub[JavaTypeable](channel, token) with JacksonMethodDescriptorFactory
    with CachingMethodDescriptorFactory[JavaTypeable]