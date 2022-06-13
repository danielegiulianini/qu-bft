package qu

import io.grpc.MethodDescriptor
import play.api.libs.json.{Format, Json}

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

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