package qu

import io.grpc.MethodDescriptor
import play.api.libs.json.{Format, Json}
import presentation.MarshallerFactory

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

trait PlayJsonMarshallerFactory extends MarshallerFactory[Format] {

  override def marshallerFor[T: Format]: MethodDescriptor.Marshaller[T] =
    new MethodDescriptor.Marshaller[T]() {
      override def stream(value: T): InputStream = {
        new ByteArrayInputStream(Json.stringify(Json.toJson(value)).getBytes(StandardCharsets.UTF_8))
      }

      override def parse(stream: InputStream): T =
        Json.parse(scala.io.Source.fromInputStream(stream).mkString).as[T]
    }
}