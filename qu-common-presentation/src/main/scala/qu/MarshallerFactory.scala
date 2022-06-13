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




trait MarshallerFactory[Marshallable[_]] {
  def marshallerFor[T: Marshallable]: MethodDescriptor.Marshaller[T]
}






