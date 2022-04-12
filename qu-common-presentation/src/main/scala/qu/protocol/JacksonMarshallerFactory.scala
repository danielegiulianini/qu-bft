package qu.protocol

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor

import java.io.{ByteArrayInputStream, InputStream}

//potrei anche banalmente estendere MethodDescriptorFactory
trait JacksonMarshallerFactory extends MarshallerFactory {
  self: MethodDescriptorFactory =>

  type Marshallable[T] = JavaTypeable[T]

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
  class JacksonOperationMixin

  val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    //this allows to plug jackson features to original class (technology-agnostic) class w/o editing it
    .addMixIn(classOf[Messages.Operation[_, _]], classOf[JacksonOperationMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    .build() :: ClassTagExtensions

  override def marshallerFor[T](implicit clz: JavaTypeable[T]): MethodDescriptor.Marshaller[T] =
    new MethodDescriptor.Marshaller[T]() {
      override def stream(value: T): InputStream = {
        new ByteArrayInputStream(mapper.writeValueAsBytes(value))
      }

      override def parse(stream: InputStream): T = {
        mapper.readValue[T](stream)
      }
    }

  //can move in another file
  def getGenericSignature[T, U](implicit javaTypeable: JavaTypeable[T],
                                javaTypeable2: JavaTypeable[U]): String =
    implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature

  //should I define a trait that implements Signaturable and implicitly import here?
  //override type Signaturable[T, U] = implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature
}

//family polymorphism (one-shot extension):
trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory with JacksonMarshallerFactory
//example of use: class JacksonCLientSTub extends ClientStub with JacksonMethodDescriptorFactory
