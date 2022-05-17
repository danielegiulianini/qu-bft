package qu


import java.util.Objects
import scala.reflect.runtime.universe._

object HeterogeneousContainer { // Typesafe heterogeneous container pattern - client
  def main(args: Array[String]): Unit = {

    val a = new HeterogeneousContainer
    a.putFavoriteWCb[Int](2)
    a.putFavoriteWCb[String]("ciao")
    a.putFavoriteWCb[List[String]](List("ciao"))

    val c = a.getFavoriteWCb[Int]
    println("c " + c)
    /*val f: Favorites = new Favorites
    f.putFavorite(classOf[String], "Java")
    f.putFavorite(classOf[Integer], 0xcafebabe)
    f.putFavorite(classOf[Class[_]], classOf[Favorites])
    val favoriteString: String = f.getFavorite(classOf[String])
    val favoriteInteger: Int = f.getFavorite(classOf[Integer])
    val favoriteClass: Class[_] = f.getFavorite(classOf[Class[_]])
    System.out.printf("%s %x %s%n", favoriteString, favoriteInteger, favoriteClass.getName)
*/
  }
}

//it 's not thred safe (must use a mutable collection...)
class HeterogeneousContainer {
  private var favorites: Map[TypeTag[_], Any] = Map() //Any or _?

  def putFavoriteWCb[T:TypeTag](instance: T): Unit =
    favorites = favorites + (Objects.requireNonNull(implicitly[TypeTag[T]]) -> instance)    //if (`type` == null) throw new NullPointerException("Type is null")

  //se c'è ritorna quello che c'è tipato, altrimenti deve ritornare un option vutoo
  def getFavoriteWCb[T:TypeTag]: Option[T] = favorites.get(implicitly[TypeTag[T]])
    .map(_.asInstanceOf[T])
}

/*
class Favorites2 {
  private val favorites: util.Map[Class[_], AnyRef] = new util.HashMap[Class[_], AnyRef]

  def putFavorite[T](`type`: Class[T], instance: T): Unit = {
    if (`type` == null) throw new NullPointerException("Type is null")
    favorites.put(`type`, instance)
  }

  def getFavorite[T](`type`: Class[T]): T = `type`.cast(favorites.get(`type`))
}*/

/*without cb:
def putFavorite[T](instance: T)(implicit `type`: TypeTag[T]): Unit =
  favorites = favorites + (Objects.requireNonNull(`type`) -> instance)    //if (`type` == null) throw new NullPointerException("Type is null")

//se c'è ritorna quello che c'è tipato, altrimenti deve ritornare un option vutoo
def getFavorite[T](implicit `type`: TypeTag[T]): Option[T] = favorites.get(`type`).map(_.asInstanceOf[T])*/