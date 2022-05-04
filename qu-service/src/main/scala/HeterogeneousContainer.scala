import scala.reflect.runtime.universe._

object HeterogeneousContainer { // Typesafe heterogeneous container pattern - client
  def main(args: Array[String]): Unit = {
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


class HeterogeneousContainer {
  private val favorites: Map[TypeTag[_], Object] = Map()

  def putFavorite[T](`type`: TypeTag[T], instance: T): Unit = {
    if (`type` == null) throw new NullPointerException("Type is null")
    favorites + (`type`-> instance)
  }

  def getFavorite[T](`type`: TypeTag[T]): T = favorites.get(`type`).asInstanceOf[T]

  // def cast[A](a: Any, tag: TypeTag[A]): A = a.asInstanceOf[A]
  //`type`.cast(favorites.get(`type`))
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