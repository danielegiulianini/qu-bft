package qu.protocol

import io.grpc.MethodDescriptor
import qu.protocol.Messages.{Request, Response}


trait MethodDescriptorFactory { self:MarshallerFactory =>
  //a deterministic function on parameter types...
  def getGenericSignature[T: Marshallable, U: Marshallable]: String //type Signaturable[T, U] <: {def getGenericSignature: String}

  //todo: here I require Marhallable of Response instead of only a marshaller of U and T so marshallerForResponse etc. are not needed in MDFactory
  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc: Marshallable[Request[T, U]], enc3: Marshallable[Response[T, U]], enc2: Marshallable[T], dec: Marshallable[U]): MethodDescriptor[Messages.Request[T, U], Messages.Response[T,U]] = {
    MethodDescriptor.newBuilder(
      marshallerFor[Messages.Request[T, U]],
      marshallerFor[Messages.Response[T,U]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build
  }
}

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
trait CachingMethodDescriptorFactory extends MethodDescriptorFactory with MarshallerFactory {
  //i need an identifier of the pair of methods
  //override abstract is required here?
  override def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                      (implicit enc: Marshallable[Request[T, U]],
                                                       enc3: Marshallable[Response[T,U]],
                                                       enc2: Marshallable[T], dec: Marshallable[U]):
  MethodDescriptor[Messages.Request[T, U], Messages.Response[T,U]] = {
    //super refers to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor(methodName, serviceName))(getGenericSignature[T, U])
  }
}

//type Marshallable[T]
//override type Marshallable[T] = Marshallable[T]
//def marshallerFor[T: Marshallable](): MethodDescriptor.Marshaller[T]
