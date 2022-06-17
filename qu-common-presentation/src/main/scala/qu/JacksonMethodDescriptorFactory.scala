package qu

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.module.scala.JavaTypeable
import presentation.MethodDescriptorFactory

//family polymorphism:
trait JacksonMethodDescriptorFactory extends MethodDescriptorFactory[JavaTypeable] with JacksonMarshallerFactory {
  override def genericTypesIdentifier[ReqT: JavaTypeable, RespT: JavaTypeable]: String =
    (typeIdentifier[ReqT]() + typeIdentifier[RespT]()).replace("/", "")

  private def typeIdentifier[T: JavaTypeable](): String = {
    implicitly[JavaTypeable[T]].asJavaType(TypeFactory.defaultInstance()).getGenericSignature
  }
}
