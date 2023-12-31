package qu

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonTypeInfo}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor
import presentation.BoxedUnitSerializer.UnitModule
import presentation.MarshallerFactory
//import presentation.UnitSerializer.UnitModule

import java.io.{ByteArrayInputStream, InputStream}
import scala.runtime.BoxedUnit

//import that declares specific dependency
import qu.model.QuorumSystemThresholdQuModel._


/**
 * An implementation of [[presentation.MarshallerFactory]] leveraging Jackson for (de)serialization technology.
 */
trait JacksonMarshallerFactory extends MarshallerFactory[JavaTypeable] {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
  class JacksonOperationMixin

  private val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .addModule(UnitModule)
    //this mixin allows to plug jackson features to original class (technology-agnostic) class w/o editing it
    .addMixIn(classOf[MyOperation[_, _]], classOf[JacksonOperationMixin])
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
