package core.particles

import scala.collection.mutable

case class ComparisonOptions(compareIntegers: Boolean, takeAllLeftKeys: Boolean, takeAllRightKeys: Boolean)

object MetaObject {

  def deepEquality(first: Any, second: Any, options: ComparisonOptions =
  new ComparisonOptions(true, true, true)): Boolean = {

    def deepEquality(first: Any, second: Any, closed: mutable.Set[(MetaObject, MetaObject)]): Boolean = {
      if (first == second)
        return true

      (first, second) match {
        case (seq1: Set[_], seq2: Set[_]) =>
          if (seq1.size != seq2.size)
            return false
          true //TODO missing checks.
        case (seq1: Seq[_], seq2: Seq[_]) =>
          if (seq1.length != seq2.length)
            return false
          seq1.zip(seq2).forall(p => deepEquality(p._1, p._2, closed))
        case (meta1: MetaObject, meta2: MetaObject) => deepEqualityMeta(meta1, meta2, closed)
        case (int1: Integer, int2: Integer) => if (options.compareIntegers) first == second else true
        case _ => first == second
      }
    }

    def deepEqualityMeta(first: MetaObject, second: MetaObject, closed: mutable.Set[(MetaObject, MetaObject)]): Boolean = {
      val key = (first, second)
      if (!closed.add(key))
        return true

      if (!first.clazz.equals(second.clazz))
        return false

      val sharedKeys = (options.takeAllLeftKeys, options.takeAllRightKeys) match {
        case (true, true) => first.data.keySet ++ second.data.keySet
        case (false, false) => first.data.keySet.intersect(second.data.keySet)
        case (true, false) => first.data.keySet
        case (false, true) => second.data.keySet
      }
      sharedKeys.forall(key => (first.data.get(key), second.data.get(key)) match {
        case (Some(firstVal), Some(secondVal)) => deepEquality(firstVal, secondVal, closed)
        case _ => false
      })
    }

    deepEquality(first, second, mutable.Set[(MetaObject, MetaObject)]())
  }

  def classDebugRepresentation(_clazz: Any): String = _clazz match {
    case string: String => string
    case anyRef: AnyRef =>
      val simpleName: String = anyRef.getClass.getSimpleName
      if (simpleName.last == '$')
        return simpleName.dropRight(1)
      simpleName
    case _ => _clazz.toString
  }
}

class MetaObject(var clazz: AnyRef, entries: (Any, Any)*) extends Dynamic {
  def transform(visited: mutable.Set[MetaObject], transformation: MetaObject => Unit) = {

    transformNode(this)
    def transformNode(metaObject: MetaObject): Unit = {
      if (!visited.add(metaObject))
        return

      transformation(metaObject)

      for(child <- metaObject.data.values)
      {
        child match {
          case metaObject: MetaObject =>
            transformNode(metaObject)
          case sequence: Seq[_] =>
            sequence.foreach({case metaObject: MetaObject => transformNode(metaObject) })
          case _ =>
        }
      }
    }
  }

  def replaceWith(metaObject: MetaObject): Unit = {
    clazz = metaObject.clazz
    data.clear()
    data ++= metaObject.data
  }

  val data: mutable.Map[Any, Any] = mutable.Map.empty
  data ++= entries

  def apply(key: Any) = data(key)

  def update(key: Any, value: Any) = data.put(key, value)

  def selectDynamic(name: String) =
    data.getOrElse(name, sys.error("member not found"))

  def updateDynamic(name: String)(value: Any) {
    data += name -> value
  }

  override def toString: String = {
    val className = MetaObject.classDebugRepresentation(clazz)
    if (data.isEmpty)
      return className
    s"$className: ${data.map(kv => (MetaObject.classDebugRepresentation(kv._1), kv._2))}"
  }

  override def equals(other: Any): Boolean = other match {
    case that: MetaObject =>
      val dataEquals: Boolean = data == that.data
      (that canEqual this) &&
        dataEquals &&
        clazz == that.clazz
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[MetaObject]

  override def hashCode(): Int = {
    val state = Seq(data, clazz)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}