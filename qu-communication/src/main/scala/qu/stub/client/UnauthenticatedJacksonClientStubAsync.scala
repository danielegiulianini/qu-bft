package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannel
import qu.{CachingMethodDescriptorFactory, JacksonMethodDescriptorFactory}

import scala.concurrent.ExecutionContext

class UnauthenticatedJacksonClientStubAsync(channel: ManagedChannel)(implicit executor: ExecutionContext)
  extends AsyncGrpcClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory
    with CachingMethodDescriptorFactory[JavaTypeable]

