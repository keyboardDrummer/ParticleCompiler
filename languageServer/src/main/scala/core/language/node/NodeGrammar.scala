package core.language.node

import core.bigrammar.BiGrammarToParser.AnyWithMap
import core.bigrammar.grammars.{FieldPosition, MapGrammarWithMap}
import core.bigrammar.printer.UndefinedDestructuringValue
import core.bigrammar.{BiGrammar, WithMap}

class NodeGrammar(inner: BiGrammar, val shape: NodeShape)
  extends MapGrammarWithMap(inner,
    input => NodeGrammar.construct(input, shape),
    obj => NodeGrammar.destruct(obj, shape))
{
  override def withChildren(newChildren: Seq[BiGrammar]): MapGrammarWithMap = new NodeGrammar(newChildren(0), shape)
}

object NodeGrammar {

  //noinspection ComparingUnrelatedTypes
  def destruct(withMap: WithMap[Any], shape: NodeShape): Option[WithMap[Any]] = {
    val value = withMap.value
    if (!value.isInstanceOf[NodeLike])
      return None

    val node = value.asInstanceOf[NodeLike]

    if (node.shape == shape) {
      val dataViewAsGenericMap = node.dataView.map(t => (t._1.asInstanceOf[Any], t._2))
      Some(WithMap(UndefinedDestructuringValue, dataViewAsGenericMap))
    }
    else {
      None
    }
  }

  case class ValueNotFound(meta: NodeLike, field: Any)

  def construct(withMap: AnyWithMap, key: NodeShape): AnyWithMap = {
    val result = new Node(key)
    result.data ++= withMap.namedValues.collect { case (k: NodeField,v) => (k,v) } // TODO trivia should be stored in a separate array on Node.
    result.sources ++= withMap.namedValues.collect { case (k: FieldPosition,v) => (k.field,v.asInstanceOf[SourceRange]) }
    WithMap(result, Map.empty)
  }
}