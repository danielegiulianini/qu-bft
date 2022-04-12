package qu.protocol

import io.grpc.MethodDescriptor
import qu.protocol.Messages.{Request, Response}


trait MethodDescriptorFactory {
  type Marshallable[T]

  //a deterministic function on parameter types...
  def getGenericSignature[T, U]: String   //type Signaturable[T, U] <: {def getGenericSignature: String}

  def marshallerFor[T](implicit clz: Marshallable[T]): MethodDescriptor.Marshaller[T]

  //todo: here I require Marhallable of Response instead of only a marshaller of U and T so marshallerForResponse etc. are not needed in MDFactory
  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc: Marshallable[Request[T, U]], enc3: Marshallable[Response[T]], enc2: Marshallable[T], dec: Marshallable[U]): MethodDescriptor[Messages.Request[T, U], Messages.Response[T]] = {
    MethodDescriptor.newBuilder(
      marshallerFor[Messages.Request[T, U]],
      marshallerFor[Messages.Response[T]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName,        // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build
  }
}

trait MarshallerFactory {
  type Marshallable[T]

  def marshallerFor[T](implicit clz: Marshallable[T]): MethodDescriptor.Marshaller[T]
}


