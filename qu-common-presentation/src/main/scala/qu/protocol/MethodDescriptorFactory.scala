package qu.protocol

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor
import qu.protocol.Messages.{Request, Response}
import java.io.{ByteArrayInputStream, InputStream}

//family polymorphism (one-shot extension): trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory with JacksonMarshallerFactory
//example of use: class JacksonCLientSTub extends ClientStub with JacksonMethodDescriptorFactory

//fam pol
trait MethodDescriptorFactory {
  type Marshallable[T]

  //a deterministic function on parameter types...
  def getGenericSignature[T, U]: String   //type Signaturable[T, U] <: {def getGenericSignature: String}

  def marshallerFor[T](implicit clz: Marshallable[T]): MethodDescriptor.Marshaller[T]

  //todo here I require Marhallable of Response instead of only a marshaller of U and T
  // so marshallerForResponse etc. are not needed in MDFactory
  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc: Marshallable[Request[T, U]], enc3: Marshallable[Response[T]], enc2: Marshallable[T], dec: Marshallable[U]): MethodDescriptor[Messages.Request[T, U], Messages.Response[T]] = {
    MethodDescriptor.newBuilder(
      marshallerFor[Messages.Request[T, U]],
      marshallerFor[Messages.Response[T]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName,
        // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build
  }
}
