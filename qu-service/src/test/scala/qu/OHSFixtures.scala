package qu

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{AsyncTestSuite, AsyncTestSuiteMixin, FutureOutcome, Suite, SuiteMixin, fixture}
import qu.RecipientInfo.id
import qu.model.ConcreteQuModel.{Key, OHS, ReplicaHistory, ServerId, emptyCandidate, emptyLT, updateAuthenticatorFor, α}
import qu.service.AbstractQuService.ServerInfo

import scala.collection.immutable.{Map, List => RH}

trait OHSServer {
  val quServer1 = RecipientInfo(ip = "ciao2", port = 1)
  val quServer2 = RecipientInfo(ip = "localhost", port = 2)
  val quServer3 = RecipientInfo(ip = "localhost", port = 3)
  val quServer4 = RecipientInfo(ip = "localhost", port = 4)

  val keysByServer: Map[ServerId, Map[ServerId, Key]] = Map(
    id(quServer1) -> Map(id(quServer1) -> "ks1s1",
      id(quServer2) -> "ks1s2",
      id(quServer3) -> "ks1s3",
      id(quServer4) -> "ks1s4"),
    id(quServer2) -> Map(id(quServer1) -> "ks2s1",
      id(quServer2) -> "ks2s2",
      id(quServer3) -> "ks2s3",
      id(quServer4) -> "ks2s4"),
    id(quServer3) -> Map(id(quServer1) -> "ks3s1",
      id(quServer2) -> "ks3s2",
      id(quServer3) -> "ks3s3",
      id(quServer4) -> "ks3s4"),
    id(quServer4) -> Map(id(quServer1) -> "ks4s1",
      id(quServer2) -> "ks4s2",
      id(quServer3) -> "ks4s3",
      id(quServer4) -> "ks4s4"))

  val quServer1WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer1)))
  val quServer2WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer2)))
  val quServer3WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer3)))
  val quServer4WithKey = ServerInfo(ip = quServer1.ip, port = quServer1.port, keySharedWithMe = keysByServer(id(quServer1))(id(quServer4)))

  val InitialObject = 2022
  val FaultyServersCount = 1
  val MalevolentServersCount = 0

}

trait OHSFixtures { self:OHSServer =>

  val rhsWithInlineMethod: Map[ServerId, ReplicaHistory] = rhsWithInline(true)
  val rhsWithInlineBarrier: Map[ServerId, ReplicaHistory] = rhsWithInline(false)

  import qu.model.ConcreteQuModel.{ConcreteLogicalTimestamp => LT}

  val rhsWithBarrier = Map[ServerId, ReplicaHistory](
    id(quServer1) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(2, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(3, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(4, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  def emptyOhsRepresentation(servers: Set[ServerId]) = Some("ohsrepr") //represent(emptyOhs(servers)))

  val aOperationRepresentation = Some("oprepr") //represent[Int, Int](Some(new GetObj[Int]())))

  val serverIds = keysByServer.keys.toSet

  val rhsWithMethodById = Map[ServerId, ReplicaHistory](
    id(quServer1) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(1, false, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  def rhsWithInline(barrierFlag: Boolean): Map[ServerId, ReplicaHistory] = Map[ServerId, ReplicaHistory](
    id(quServer1) -> RH(
      emptyCandidate,
      (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
        emptyLT)),
    id(quServer2) ->
      RH(
        emptyCandidate,
        (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer3) ->
      RH(
        emptyCandidate,
        (LT(2, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
    id(quServer4) ->
      RH(
        emptyCandidate,
        (LT(1, barrierFlag, Some("client1"), aOperationRepresentation, emptyOhsRepresentation(serverIds)),
          emptyLT)),
  )

  //  override type ReplicaHistory = SortedSet[Candidate]
  //  override type α = Map[ServerId, HMAC]
  //  override type AuthenticatedReplicaHistory = (ReplicaHistory, α)
  //  override type OHS = Map[ServerId, AuthenticatedReplicaHistory]

  def generateOhsFromRHsAndKeys(rhs: Map[ServerId, ReplicaHistory], keys: Map[ServerId, Map[ServerId, Key]]): OHS =
    keys.map { case (id, keys) => id -> (rhs(id), updateAuthenticatorFor(keys)(id)(rhs(id))) }

  val ohsWithMethod: OHS = generateOhsFromRHsAndKeys(rhsWithMethodById, keysByServer)
  val ohsWithInlineMethod: OHS = generateOhsFromRHsAndKeys(rhsWithInlineMethod, keysByServer)
  val ohsWithInlineBarrier: OHS = generateOhsFromRHsAndKeys(rhsWithInlineBarrier, keysByServer)
  val ohsWithBarrier: OHS = generateOhsFromRHsAndKeys(rhsWithBarrier, keysByServer)

  /*
    println("****rhs WITH METHOD*****")
    println(rhsWithMethodById)

    println("****rhs WITH INLINE METHOD*****")
    println(rhsWithInlineMethod)

    println("****rhs WITH INLINE BARRIER*****")
    println(rhsWithInlineBarrier)

    println("****rhs WITH BARRIER*****")
    println(rhsWithBarrier)
    /*----------------------------------*/
    println("****OHS WITH INVALID AUTHENTICATOR*****")
    //println(ohsWithInvalidAuthenticatorFor(id(quServer1)))

    println("****OHS WITH METHOD*****")
    println(ohsWithMethod)

    println("****OHS WITH INLINE METHOD*****")
    println(ohsWithInlineMethod)

    println("****OHS WITH INLINE BARRIER*****")
    println(ohsWithInlineBarrier)

    println("****OHS WITH BARRIER*****")
    println(ohsWithBarrier)

    println("****OHS WITH INVALID AUTHENTICATOR*****")
    //println(ohsWithInvalidAuthenticatorFor(id(quServer1)))*/


  def invalidateAuthenticatorForServer(serverId: ServerId): α = {
    val (_, originalAuthenticator) = ohsWithInlineMethod(serverId)
    originalAuthenticator.map {
      case (id, _) if id == serverId => id -> "corrupted"
    }
  }

  /*def ohsWithInvalidAuthenticatorFor(serverId: ServerId): OHS =
    ohsWithMethod.map { case (sid, (rh, _)) if sid == serverId => (sid, (rh, invalidateAuthenticatorForServer(sid))) }
*/

}


import org.scalatest._
/*
trait Builder extends AsyncTestSuiteMixin { this: AsyncTestSuite =>

  val builder = new StringBuilder

  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    builder.append("ScalaTest is ")
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally builder.clear()
  }
}

trait Buffer extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: fixture.NoArg) = {
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally buffer.clear()
  }
}

*/
import org.scalatest._
import collection.mutable.ListBuffer
import org.scalatest._
import collection.mutable.ListBuffer
/*
import org.scalatest.flatspec.FixtureAnyFlatSpecLike//FixtureFlatSpecLike



trait Builder extends SuiteMixin with FixtureAnyFlatSpecLike { this: Suite =>

  val builder = new StringBuilder

  abstract override def withFixture(test: NoArgTest) = {
    builder.append("ScalaTest is ")
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally builder.clear()       // Shared cleanup (run at end of each test)

  }
}

trait Buffer extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest) = {
    try super.withFixture(test) // To be stackable, must call super.withFixture
    finally buffer.clear()
  }
}

class ExampleSpec extends AnyFlatSpec with Builder with Buffer {

  "Testing" should "be easy" in {
    builder.append("easy!")
    assert(builder.toString === "ScalaTest is easy!")
    assert(buffer.isEmpty)
    buffer += "sweet"
  }

  it should "be fun" in {
    builder.append("fun!")
    assert(builder.toString === "ScalaTest is fun!")
    assert(buffer.isEmpty)
    buffer += "clear"
  }
}


import org.scalatest._
import collection.mutable.ListBuffer

trait Builder extends BeforeAndAfterEach { this: Suite =>

  val builder = new StringBuilder

  override def beforeEach() {
    builder.append("ScalaTest is ")
    super.beforeEach() // To be stackable, must call super.beforeEach
  }

  override def afterEach() {
    try super.afterEach() // To be stackable, must call super.afterEach
    finally builder.clear()
  }
}

trait AsyncStub extends SuiteMixin { this: Suite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest) = {
  try super.withFixture(test) // To be stackable, must call super.withFixture
  finally buffer.clear()
}
}*/