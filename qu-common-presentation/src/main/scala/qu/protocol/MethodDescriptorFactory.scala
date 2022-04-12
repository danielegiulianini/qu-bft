package qu.protocol

import io.grpc.MethodDescriptor
import qu.protocol.Messages.{Request, Response}

trait MethodDescriptorFactory {
  type Marshallable[T]

  //a deterministic function on parameter types...
  def getGenericSignature[T: Marshallable, U: Marshallable]: String //type Signaturable[T, U] <: {def getGenericSignature: String}

  def marshallerFor[T: Marshallable](): MethodDescriptor.Marshaller[T]

  //todo: here I require Marhallable of Response instead of only a marshaller of U and T so marshallerForResponse etc. are not needed in MDFactory
  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc: Marshallable[Request[T, U]], enc3: Marshallable[Response[T]], enc2: Marshallable[T], dec: Marshallable[U]): MethodDescriptor[Messages.Request[T, U], Messages.Response[T]] = {
    MethodDescriptor.newBuilder(
      marshallerFor[Messages.Request[T, U]],
      marshallerFor[Messages.Response[T]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build
  }
}

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
trait CachingMethodDescriptorFactory extends MethodDescriptorFactory {

  //i need an identifier of the pair of methods
  override abstract def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                      (implicit enc: Marshallable[Request[T, U]],
                                                       enc3: Marshallable[Response[T]],
                                                       enc2: Marshallable[T], dec: Marshallable[U]):
  MethodDescriptor[Messages.Request[T, U], Messages.Response[T]] = {
    //super referes to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor(methodName, serviceName)).apply(this.getGenericSignature[T, U])
  }
}


trait MarshallerFactory {
  type Marshallable[T]

  def marshallerFor[T: Marshallable]: MethodDescriptor.Marshaller[T]
}


