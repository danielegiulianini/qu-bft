package qu.protocol

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.module.scala.JavaTypeable
import io.grpc.MethodDescriptor

//import that declares specific dependency
import qu.protocol.ConcreteQuModel._


trait MethodDescriptorFactory[Marshallable[_]] {
  self: MarshallerFactory[Marshallable] =>

  //a deterministic function on parameter types...//todo can also be refactored to be only on 1 type parameter
  def getGenericSignature[T: Marshallable, U: Marshallable]: String //type Signaturable[T, U] <: {def getGenericSignature: String}

  //todo: here I require Marhallable of Response instead of only a marshaller of U and T
  // so marshallerForResponse etc. are not needed in MDFactory
  def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                    (implicit enc: Marshallable[Request[T, U]],
                                     enc3: Marshallable[Response[T, U]],
                                     enc2: Marshallable[T],
                                     dec: Marshallable[U]):
  MethodDescriptor[Request[T, U], Response[T, U]] =
    MethodDescriptor.newBuilder(
      marshallerFor[Request[T, U]],
      marshallerFor[Response[T, U]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build

  def generateMethodDescriptor3[T, U, TF[_, _], UF[_, _]](methodName: String, serviceName: String)
                                                         (implicit enc: Marshallable[TF[T, U]],
                                                          enc3: Marshallable[UF[T, U]],
                                                          enc2: Marshallable[T],
                                                          dec: Marshallable[U]):
  MethodDescriptor[TF[T, U], UF[T, U]] = //MethodDescriptor[Messages.Request[T, U], Messages.Response[T, U]] = {
    MethodDescriptor.newBuilder(
      marshallerFor[TF[T, U]],
      marshallerFor[UF[T, U]])
      .setFullMethodName(MethodDescriptor.generateFullMethodName(serviceName, // methodName + implicitly[Signaturable[T, U]].getGenericSignature))
        methodName + getGenericSignature[T, U]))
      .setType(MethodDescriptor.MethodType.UNARY)
      .setSampledToLocalTracing(true)
      .build

  /* should and could here, but put in subclasses for passing them the burden (instead of lib user)
  of definig marshaller for Response and Request
  def generateRequestResponseMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                   (implicit enc: Marshallable[Request[T, U]],
                                                    enc3: Marshallable[Response[T,U]],
                                                    enc2: Marshallable[T],
                                                    dec: Marshallable[U]) =
  generateMethodDescriptor3[T, U, Request, Response](methodName, serviceName)*/


  //FACADE for reducing implicits at call side (could go in new mixin)
  //could possibly be moved to subclasses only? no!
  /*def generateRequestResponseMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                   (implicit enc2: Marshallable[T],
                                                    dec: Marshallable[U]):
  MethodDescriptor[Request[T, U], Response[T, U]]

  import qu.protocol.ConcreteQuModel._

  def generateObjectSyncMethodDescriptor[T, U](methodName: String, serviceName: String)
                                              (implicit enc2: Marshallable[T],
                                               dec: Marshallable[U]):
  MethodDescriptor[MyLogicalTimestamp[T, U], ObjectSyncResponse[U]]*/
}

//an optimization that leverages flyweight pattern to avoid regenerating method descriptors
trait CachingMethodDescriptorFactory[Marshallable[_]] extends MethodDescriptorFactory[Marshallable]
  with MarshallerFactory[Marshallable] {
  //i need an identifier of the pair of methods
  //override abstract is required here?
  override def generateMethodDescriptor[T, U](methodName: String, serviceName: String)
                                             (implicit enc: Marshallable[Request[T, U]],
                                              enc3: Marshallable[Response[T, U]],
                                              enc2: Marshallable[T], dec: Marshallable[U]):
  MethodDescriptor[Request[T, U], Response[T, U]] =
  //super refers to the next in the chain
    MemoHelper.memoize((_: String) =>
      super.generateMethodDescriptor(methodName, serviceName))(getGenericSignature[T, U])
}


//family polymorphism:
trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory[JavaTypeable] with JacksonMarshallerFactory {
  override def getGenericSignature[T: JavaTypeable, U: JavaTypeable]: String = //todo to be refactored
    (implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature +
      implicitly[JavaTypeable[U]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature).replace("/", "") //todo

  //should I define a trait that implements Signaturable and implicitly import here?  //override type Signaturable[T, U] = implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature

  //facade for reducing implicits at call side (could go in new mixin)
  /*override def generateRequestResponseMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                            (implicit enc2: JavaTypeable[T], dec: JavaTypeable[U]) =
    this.generateMethodDescriptor3(methodName, serviceName)*/

  /*override def generateObjectSyncMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc2: JavaTypeable[T], dec: JavaTypeable[U]) =
    this.generateMethodDescriptor3[T, U, MyLogicalTimestamp, ObjectSyncResponse](methodName, serviceName)*/

}
