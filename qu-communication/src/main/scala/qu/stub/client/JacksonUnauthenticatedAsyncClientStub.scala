package qu.stub.client

import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.ManagedChannel
import presentation.CachingMethodDescriptorFactory
import qu.JacksonMethodDescriptorFactory

import scala.concurrent.ExecutionContext


/**
 * An implementation of [[qu.stub.client.AsyncClientStub]] leveraging Jackson (so, JSON)
 * as (de)serialization technology.
 */
class JacksonUnauthenticatedAsyncClientStub(channel: ManagedChannel)(implicit executor: ExecutionContext)
  extends AbstractAsyncClientStub[JavaTypeable](channel) with JacksonMethodDescriptorFactory
    with CachingMethodDescriptorFactory[JavaTypeable]

