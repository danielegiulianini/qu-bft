package qu.protocol

//interface required for objects contained in replicas
trait DeepCloneable[ObjectT] {
  def deepClone(): ObjectT
}
