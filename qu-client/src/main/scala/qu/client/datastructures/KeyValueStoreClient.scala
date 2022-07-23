package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.SocketAddress
import qu.client.{AuthenticatingClient, QuClient}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class GetObj[ObjectT] extends QueryReturningObject[ObjectT]

class Get[K, V](key: K) extends Query[Option[V], MutableMap[K, V]] {
  override def whatToReturn(obj: MutableMap[K, V]): Option[V] = obj.get(key)
}

class AddOne[K, V](elem: (K, V)) extends UpdateReturningUnUpdatedObject[MutableMap[K, V]] {
  override def updateObject(obj: MutableMap[K, V]): MutableMap[K, V] = obj.addOne(elem)
}

class SubtractOne[K, V](key: K) extends UpdateReturningUnUpdatedObject[MutableMap[K, V]] {
  override def updateObject(obj: MutableMap[K, V]): MutableMap[K, V] = obj.subtractOne(key)
}


case class KeyValueStoreClient[K, V](username: String,
                                password: String,
                                authServerIp: String,
                                authServerPort: Int,
                                serversInfo: Set[SocketAddress],
                                thresholds: QuorumSystemThresholds,
                                maxTimeToWait: Duration = 100.seconds)(implicit executionContext: ExecutionContext)
  extends AuthenticatedQuClient[MutableMap[K, V]](username,
    password,
    authServerIp,
    authServerPort,
    serversInfo,
    thresholds) with MutableMap[K, V] {

  //async APIs
  def getAsync(key: K): Future[Option[V]] = submit(new Get(key))

  def iteratorAsync: Future[Iterator[(K, V)]] =
    for {
      map <- submit(new GetObj[MutableMap[K, V]]())
    } yield map.iterator

  def addOneAsync(elem: (K, V)): Future[MutableMap[K, V]] = submit(new AddOne(elem))

  def subtractOneAsync(elem: K): Future[MutableMap[K, V]] = submit(new SubtractOne[K, V](elem))

  //sync versions
  override def get(key: K): Option[V] = await(getAsync(key))

  override def addOne(elem: (K, V)): KeyValueStoreClient.this.type = {
    await(addOneAsync(elem))
    this
  }

  override def subtractOne(elem: K): KeyValueStoreClient.this.type = {
    await(subtractOneAsync(elem))
    this
  }

  override def iterator: Iterator[(K, V)] = await(iteratorAsync)
}
