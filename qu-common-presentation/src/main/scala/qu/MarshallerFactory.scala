package qu

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor
import play.api.libs.json.{Format, Json}
import qu.model.StatusCode

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets
import scala.runtime.BoxedUnit

//import that declares specific dependency
import qu.model.ConcreteQuModel._


trait MarshallerFactory[Marshallable[_]] {
  def marshallerFor[T: Marshallable]: MethodDescriptor.Marshaller[T]
}


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

//could inherit from MethodDescriptorFactory
trait PlayJsonMarshallerFactory extends MarshallerFactory[Format] {

  override def marshallerFor[T: Format]: MethodDescriptor.Marshaller[T] =
    new MethodDescriptor.Marshaller[T]() {
      override def stream(value: T): InputStream = {
        new ByteArrayInputStream(Json.stringify(Json.toJson(value)).getBytes(StandardCharsets.UTF_8)) //this.gson2 = generateGson(clz)//new ByteArrayInputStream(gson.toJson(value, clz).getBytes(StandardCharsets.UTF_8))
      }

      override def parse(stream: InputStream): T =
        Json.parse(scala.io.Source.fromInputStream(stream).mkString).as[T] //import play.api.libs.json._      //return gson = generateGson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), clz)
    }
}


/*
trait PlayJsonMethodDescriptorFactory extends MethodDescriptorFactory[Format] with PlayJsonMarshallerFactory {
  override def getGenericSignature[T: Format, U: Format]: String = "ciaociao"//todo to be refactored
   /*(implicitly[Format[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature +
      implicitly[Format[U]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature).replace("/", "")*/ //todo

  //should I define a trait that implements Signaturable and implicitly import here?  //override type Signaturable[T, U] = implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature

  //facade for reducing implicits at call side (could go in new mixin)
  /*override def generateRequestResponseMethodDescriptor[T, U](methodName: String, serviceName: String)
                                                            (implicit enc2: JavaTypeable[T], dec: JavaTypeable[U]) =
    this.generateMethodDescriptor3(methodName, serviceName)*/

  /*override def generateObjectSyncMethodDescriptor[T, U](methodName: String, serviceName: String)(implicit enc2: JavaTypeable[T], dec: JavaTypeable[U]) =
    this.generateMethodDescriptor3[T, U, MyLogicalTimestamp, ObjectSyncResponse](methodName, serviceName)*/
}
*/
