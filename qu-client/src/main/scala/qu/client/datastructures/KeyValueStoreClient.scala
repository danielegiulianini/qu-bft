package qu.client.datastructures

import com.fasterxml.jackson.module.scala.JavaTypeable
import qu.client.{AuthenticatingClient, QuClient}
import qu.model.ConcreteQuModel._
import qu.model.QuorumSystemThresholds

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}


class Get[K, V](key: K) extends Query[Option[V], MutableMap[K, V]] {
  override def whatToReturn(obj: MutableMap[K, V]): Option[V] = obj.get(key)
}

//could refactor method that returns updated object
class AddOne[K, V](elem: (K, V)) extends UpdateReturningUpdatedObject[MutableMap[K, V]] {
  override def updateObject(obj: MutableMap[K, V]): MutableMap[K, V] = obj.addOne(elem)
}

class SubtractOne[K, V](key: K) extends UpdateReturningUpdatedObject[MutableMap[K, V]] {
  override def updateObject(obj: MutableMap[K, V]): MutableMap[K, V] = obj.subtractOne(key)
}


// exeriment with MutableMap
//todo complessità da spostare sul builder/factory ed eventualmente costruttore privato
class KeyValueStoreClient[K, V](username: String,
                                password: String,
                                authServerIp: String,
                                authServerPort: Int,
                                serversInfo: Map[String, Int],
                                thresholds: QuorumSystemThresholds)(implicit executionContext: ExecutionContext)
  extends AbstractStateMachine[MutableMap[K, V]](username,
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

  /* //private utilities
   type MapOperation[ReturnValueT] = Operation[ReturnValueT, MutableMap[K, V]]*/
}


object TT extends App {
  import scala.concurrent.ExecutionContext.Implicits.global

  val a = new KeyValueStoreClient[String, Int]("username", "ok", null, 2, null, null)
  a.getAsync("iao")
}


/* very interesting experiments with immutable collections, but change are required to allow to change server side
data type object:
object Utility {
  //could be used in submit
  type MapOperation[ReturnValueT, K, V] = Operation[ReturnValueT, Map[K, V]]
}

trait QueryObject[ObjectT] extends Query[ObjectT, ObjectT] {
  override def query(obj: ObjectT): ObjectT = obj
}

//reusable utility that returns object at server side
class GetObj[ObjectT]() extends QueryObject[ObjectT]

trait OperationReturningObject[ObjectT] extends Operation[ObjectT, ObjectT] {
  override def apply(obj: ObjectT): (ObjectT, ObjectT) = {
    val updatedObj = workOnObject(obj)
    (updatedObj, updatedObj)
  }

  def workOnObject(obj: ObjectT): ObjectT
}


class Get[K, V](key: K) extends Query[Option[V], Map[K, V]] {
  override def query(obj: Map[K, V]): Option[V] = obj.get(key)
}


//could refactor method that returns updated object

class Updated[K, V, V1 >: V](key: K, value: V1) extends Update[Map[K, V], Map[K, V1]] {
  override def apply(obj: Map[K, V1]): (Map[K, V1], Map[K, V]) = (obj.updated(key, value),
}


class Removed[K, V](key: K) extends Update[Map[K, V], Map[K, V]] with OperationReturningObject[Map[K, V]] {
  override def workOnObject(obj: Map[K, V]): Map[K, V] = obj.removed(key)
}

class KeyValueStoreClient2[K, V](username: String,
                                 password: String,
                                 authServerInfo: RecipientInfo,
                                 serversInfo: Set[RecipientInfo],
                                 thresholds: QuorumSystemThresholds)(implicit ec: ExecutionContext) extends Map[K, V] {
  val clientFuture: Future[QuClient[Map[K, V], JavaTypeable]] =
    new StartingClient[Map[K, V]](authServerInfo, username, password).authorize(serversInfo, thresholds)

  //async APIs
  def getAsync(key: K): Future[Option[V]] = submit(new Get(key))

  def iteratorAsync(): Future[Iterator[(K, V)]] =
    for {
      map <- submit(new GetObj())
    } yield map.iterator

  def updatedAsync[V1 >: V](key: K, value: V1): Future[Map[K, V1]] = submit(new Updated(key, value))

  def removedAsync(key: K): Future[Map[K, V]] = submit(new Removed(key))


  //sync APIs
  override def removed(key: K): Map[K, V] = await(removedAsync(key))

  override def updated[V1 >: V](key: K, value: V1): Map[K, V1] = await(updatedAsync(key, value))

  override def get(key: K): Option[V] = await(getAsync(key))

  override def iterator: Iterator[(K, V)] = await(iteratorAsync)

  //private utilities
  private def await[T](future: Future[T]) = Await.result(future, 100.millis)

  private def submit[ReturnValueT](operation: Operation[ReturnValueT, Map[K, V]]) =
    clientFuture.flatMap(_.submit(operation))
}

class KeyVal3[K, V] extends mutable.HashMap[K, V] {

}*/
