package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannel
import presentation.CachingMethodDescriptorFactory
import qu.JacksonMethodDescriptorFactory

import scala.concurrent.ExecutionContext

class UnauthenticatedJacksonAsyncClientStub(channel: ManagedChannel)(implicit executor: ExecutionContext)
  extends AsyncClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory
    with CachingMethodDescriptorFactory[JavaTypeable]

