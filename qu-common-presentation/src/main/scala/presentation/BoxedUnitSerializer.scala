package presentation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import presentation.BoxedUnitSerializer.BoxedUnitRepr

import scala.runtime.BoxedUnit

object BoxedUnitSerializer {
  val BoxedUnitRepr = "unitRepr"

  val UnitModule: SimpleModule = new SimpleModule("UnitAndBoxedUnitModule")
    .addSerializer(classOf[BoxedUnit], new BoxedUnitSerializer)
    .addDeserializer(classOf[BoxedUnit], new BoxedUnitDeserializer)
    .addSerializer(classOf[Unit], new UnitSerializer)
    .addDeserializer(classOf[Unit], new UnitDeserializer)
}

//used (instead of UnitSerializer) as automating boxing is performed by scala compiler
class BoxedUnitSerializer extends StdSerializer[BoxedUnit](classOf[BoxedUnit]) {

  import com.fasterxml.jackson.core.JsonGenerator
  import com.fasterxml.jackson.databind.SerializerProvider

  override def serialize(value: BoxedUnit, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeString(BoxedUnitRepr)
  }
}

//used when lib user explicitly passes Unit as generic parameter
class BoxedUnitDeserializer extends StdDeserializer[BoxedUnit](classOf[BoxedUnit]) {

  override def deserialize(p: JsonParser, ctx: DeserializationContext): BoxedUnit = {
    p.getText match {
      case BoxedUnitRepr => BoxedUnit.UNIT
    }
  }
}

//never used as automating boxing is performed by scala compiler
class UnitSerializer extends StdSerializer[Unit](classOf[Unit]) {

  import com.fasterxml.jackson.core.JsonGenerator
  import com.fasterxml.jackson.databind.SerializerProvider

  override def serialize(value: Unit, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeString(BoxedUnitRepr)
  }
}

//used when lib user explicitly passes Unit as generic parameter
class UnitDeserializer extends StdDeserializer[Unit](classOf[Unit]) {

  override def deserialize(p: JsonParser, ctx: DeserializationContext): Unit = {
    p.getText match {
      case BoxedUnitRepr => ()
    }
  }
}
