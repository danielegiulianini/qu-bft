package qu.protocol

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule, JavaTypeable}
import io.grpc.MethodDescriptor

import java.io.{ByteArrayInputStream, InputStream}

object JacksonMethodDescriptorFactory extends MethodDescriptorFactory {

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
  class JacksonOperationMixin

  val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule)
    .addMixIn(classOf[Messages.Operation[_,_]], classOf[JacksonOperationMixin])
    //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //QUESTO è FACOLTATIVO
    .build() :: ClassTagExtensions

  private def marshallerForWithJacksonScalaNoAnn[T](implicit clz: JavaTypeable[T]): MethodDescriptor.Marshaller[T] =
    new MethodDescriptor.Marshaller[T]() {
      //private var gson2 = null
      override def stream(value: T): InputStream = {
        new ByteArrayInputStream(mapper.writeValueAsBytes(value)) //new ByteArrayInputStream(value.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))
      }

      override def parse(stream: InputStream): T = {
        mapper.readValue[T](stream) //Json.parse(scala.io.Source.fromInputStream(stream).mkString).as[T] //import play.api.libs.json._      //return gson = generateGson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), clz)
      }
    }

  def marshallerForWithJacksonScalaForCreateResponseWithAnn[T](implicit clz: JavaTypeable[T]):
  MethodDescriptor.Marshaller[Messages.Response[T]] = {
    marshallerForWithJacksonScalaNoAnn[Messages.Response[T]]
  }

  def marshallerForWithJacksonScalaForWrappingWithAnn[T, U](implicit clz: JavaTypeable[T], clz2: JavaTypeable[U]):
  MethodDescriptor.Marshaller[Messages.Request[T, U]] = {
    marshallerForWithJacksonScalaNoAnn[Messages.Request[T, U]]
  }

}
