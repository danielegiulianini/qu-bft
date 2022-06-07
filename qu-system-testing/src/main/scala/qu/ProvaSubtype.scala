package qu

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import qu.model.ConcreteQuModel._
import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => LT}

import scala.collection.SortedSet

object ProvaSubtype extends App {
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "className")
  class JacksonOperationMixin

  /* @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "type")
   class JacksonRequestMixin*/

  private val mapper = JsonMapper.builder()
    .addModule(DefaultScalaModule) //.activateDefaultTyping()
    // .enableDefaultTyping(ObjectMapper.De ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++                                                                                                                                                                                                                                                                                                           + faultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY)
    //this mixin allows to plug jackson features to original class (technology-agnostic) class w/o editing it
    .addMixIn(classOf[MyOperation[_, _]], classOf[JacksonOperationMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    //.addMixIn(classOf[Request[_, _]], classOf[JacksonRequestMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    .addMixIn(classOf[MioOperation], classOf[JacksonOperationMixin]) //.registerSubtypes(classOf[Messages.AInterface[_, _]], classOf[Messages.C1])    //not needed
    .build() :: ClassTagExtensions

  /* val ohsSerialized = mapper.writeValueAsString(emptyOhs(Set("ciao")))
   println("ohs serialized: " + ohsSerialized)

    val ohsDeserialized = mapper.readValue(ohsSerialized)
   println("ohs deserialized: " + ohsDeserialized)
*/

  class MyQuery extends Query[Int, Int] {
    override def whatToReturn(obj: Int): Int = obj
  }

  class Increment extends UpdateReturningUnit[Int] {
    override def updateObject(obj: Int): Int = obj + 1
  }

  trait MioOperation {
    def compute(): Unit
  }

  class ConcreteMioOperation extends MioOperation {
    override def compute(): Unit = println("ciao")
  }
/*
  case class MyRequest4[ReturnValueT, ObjectT](ohs: NewOhs
                                              )*/


  case class MyRequest3[ReturnValueT, ObjectT](ohs: Int)

  case class MyRequest2[ReturnValueT, ObjectT](ohs: OHS)

  case class MyRequest[ReturnValueT, ObjectT](operation: Option[MioOperation], //[ReturnValueT, ObjectT]],
                                              ohs: OHS)

  //todo questa va
  case class MyRequest5[ReturnValueT, ObjectT](operation: Option[MioOperation], //[ReturnValueT, ObjectT]],
                                               ohs: OHS2)

  case class MyRequest6[ReturnValueT, ObjectT](operation: Option[MyOperation[ReturnValueT, ObjectT]], //[ReturnValueT, ObjectT]],
                                               ohs: OHS2)
println("la ***********serialized map: " + mapper.writeValueAsString(Map[Int, Int]()))

  type OHS2 = Map[String, (List[(LT, LT)], Map[String, String])]
  //   OHS  = Map[String, (SortedSet[(LT, LT)], Map[String, String]) >

  /** val request = MyRequest[Unit, Int](
   * operation = Some(new ConcreteMioOperation()),
   * ohs = emptyOhs(Set("ciao"))// Map("ciao" -> emptyAuthenticatedRh)
   * ) */
  /*
    val request = MyRequest2[Unit, Int](
      ohs = emptyOhs(Set("ciao"))// Map("ciao" -> emptyAuthenticatedRh)
    )*/

  /*val request = MyRequest4[Unit, Int](
    ohs = Map("ciao" -> (SortedSet(1), 2))// Map("ciao" -> emptyAuthenticatedRh)
  )*/

  /*val request = MyRequest5[Unit, Int](
    operation = Some(new ConcreteMioOperation()),
    // Map[String,(List[(LT,LT)], Map[String, String])]
    ohs = Map("ciao" -> (List(emptyCandidate), Map("ciao"-> "ciao")))   // Map("ciao" -> emptyAuthenticatedRh)
  )*/

  val request = MyRequest6[Unit, Int](
    operation = Some(new Increment()), // Map[String,(List[(LT,LT)], Map[String, String])]
//questo usa al posto
    ohs = Map("ciao" -> (List(emptyCandidate), Map("ciao" -> "ciao"))) // Map("ciao" -> emptyAuthenticatedRh)
  )

  //type  Ohs = Map[String,(List[(LT,LT)], Map[String, String])]
  //type NewOhs = Map[String, (SortedSet[Int], Int)]

  /*val requestSerialized = mapper.writeValueAsString(emptyLT)
  println("requestSerialized: " + requestSerialized)
  val requestDeserialized = mapper.readValue[ConcreteLogicalTimestamp](requestSerialized)
  println("requestDeserialized: " + requestDeserialized)
*/
  /*val request = MyRequest3[Unit, Int](
    ohs = 2// Map("ciao" -> emptyAuthenticatedRh)
  )*/

  // Map<String, (SortedSet[(LT, LT)], Map[String, String]>

  /*val request = Request[Unit, Int](
    operation = Some(new Increment()),
    ohs = Map("ciao" -> emptyAuthenticatedRh)
  )/*
  val request2 = Request[Int, Int](
    operation = Some(new MyQuery()),
    ohs = Map("ciao" -> emptyAuthenticatedRh)
  )*/
  val requestSerialized = mapper.writeValueAsString(request)
  println("requestSerialized: " + requestSerialized)
  //val requestDeserialized: Request[Unit, Int] = mapper.readValue[Request[Unit, Int]](requestSerialized)
  //println("requestDeserialized: " + requestDeserialized)
  //val requestSerialized2 = mapper.writeValueAsString(request2)
  //println("requestSerialized: " + requestSerialized2)
  // val requestDeserialized2: Request[Int, Int] = mapper.readValue[Request[Int, Int]](requestSerialized2)
*/
  val requestSerialized = mapper.writeValueAsString(request)
  println("requestSerialized: " + requestSerialized)
  val requestDeserialized = mapper.readValue[MyRequest6[Unit, Int]](requestSerialized)
  println("requestDeserialized: " + requestDeserialized)
  /*  val a = (2, "ciao")
  val requestSerialized = mapper.writeValueAsString(a)
    println("requestSerialized: " + requestSerialized)
    val requestDeserialized = mapper.readValue[(Int, String)](requestSerialized)
    println("requestDeserialized: " + requestDeserialized)*//*
  println("requestSerialized: " + requestSerialized)
  val requestDeserialized= mapper.readValue[MyRequest[Unit, Int]](requestSerialized)
  println("requestDeserialized: " + requestDeserialized)*/


}


/*
  val immutableA = Map[String, Int]("a" -> 1, "b" -> 2)
  val immutableASer = mapper.writeValueAsString(immutableA)
  println("a serialized: " + immutableASer)
  println("requestDeserialized: " + mapper.readValue(immutableASer))*/


/*
  import scala.collection.mutable.{Map => MutableMap}
  val mutableA = MutableMap().addOne("w" -> 2)
  val mutableASer = mapper.writeValueAsString(mutableA)
  println("a serialized: " + mutableASer)*/
