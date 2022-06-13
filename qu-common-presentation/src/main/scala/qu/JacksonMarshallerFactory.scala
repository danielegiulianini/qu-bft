package qu

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor

import java.io.{ByteArrayInputStream, InputStream}
import scala.runtime.BoxedUnit

//import that declares specific dependency
import qu.model.ConcreteQuModel._

//could inherit from MethodDescriptorFactory
trait JacksonMarshallerFactory extends MarshallerFactory[JavaTypeable] {
  /*  class OperationsDeserializer extends JsonDeserializer[Operations] {

    override def deserialize(p: JsonParser, ctxt: DeserializationContext): Operations = {
      p.getValueAsString match {
        case Foo.jsonValue => Foo
        case Bar.jsonValue => Bar
        case value => throw new IllegalArgumentException(s"Undefined deserializer for value: $value")
      }
    }
  }*/

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
  class JacksonOperationMixin

  private val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    //this mixin allows to plug jackson features to original class (technology-agnostic) class w/o editing it
    .addMixIn(classOf[MyOperation[_, _]], classOf[JacksonOperationMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    //.addMixIn(classOf[StatusCode], classOf[JacksonOperationMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    .addMixIn(classOf[BoxedUnit], classOf[JacksonOperationMixin])
    .build() :: ClassTagExtensions

  override def marshallerFor[T: JavaTypeable]: MethodDescriptor.Marshaller[T] =
    new MethodDescriptor.Marshaller[T]() {
      override def stream(value: T): InputStream = {
        new ByteArrayInputStream(mapper.writeValueAsBytes(value))
      }

      override def parse(stream: InputStream): T =
        mapper.readValue[T](stream)
    }
}
