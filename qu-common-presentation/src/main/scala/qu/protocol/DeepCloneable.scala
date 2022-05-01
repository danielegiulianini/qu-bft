package qu.protocol


trait DeepCloneable[ObjectT] {
  def deepClone(): ObjectT
}
