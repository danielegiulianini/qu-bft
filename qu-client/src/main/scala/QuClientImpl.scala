import qu.protocol.ConcreteQuModel._

import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global

abstract class QuClientImpl[U](policy: QuorumPolicy[U], protected var ohs: OHS[U]) extends QuClient[U] {//with Servers[U]

  override def submit[T](op: Operation[T, U]): Future[T] = {
    for {
      response <- policy.quorum(op, ohs)
      answer <- if (response._2 < 5) for {
        _ <- repair(response._3) //tis updates ohs
        a <- submit(op)
      } yield a else Future(response._1)
    } yield answer
  }

  def repair(ohs: OHS[U]): Future[Unit] = ???
}

//companion object: consider putting utilities in QuClient if they are reusable at a higher level
object QuClientImpl {
  //type ServerRefs[U] = Map[ServerId, JacksonClientStub[U]]
  //val io = new QuClientImpl[Int] with SimpleBroadcastPolicy[Int]

  //grpc-aware factory:
  def apply[U](quorumThreshold: Int,
               repairableThreshold: Int,
               ips: Set[String] //serverRefs: ServerRefs[U] //here i need IPs

               //grpc's callOptions , key management??  (to pass to clientStub...)
              ): QuClientImpl[U] = {
    //istanzio i servers (client stub) da passare alla quorumPolicy

    null
  }

  /*def apply[U](quorumThreshold: Int,
               repairableThreshold: Int,
               ipsWithKeyPaths: Set[(String, String)]
               //serverRefs: ServerRefs[U] //here i need IPs
               //grpc's callOptions , key management??  (to pass to clientStub...)
              ): QuClientImpl[U] = {
    null
  }*/
  //una factory di QUClient... istanziando lo specifico
  //un oggetto per creare il quClient
}
/*
trait Servers[U] {
  val servers: Map[ConcreteQuModel.ServerId, JacksonClientStub[U]] = Map[ServerId, JacksonClientStub[U]]()
}*/

object ProvaUserSide {

  //QuClientImpl(cluster)

  //QuClientImpl(cluster, thresholds)

  //QuClientImpl(thresholds,
  //ips = Set(("www.google.com", port = 2), ("www.amazon.com", port=80))
  QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ips = Set("www.google.com", "www.amazon.com"))

  /*QuClientImpl(quorumThreshold = 1,
    repairableThreshold = 2,
    ipsWithKeyPaths = Set(("www.google.com", "pathForGoogle"), ("www.amazon.com", "pathForAmazon")))*/
}