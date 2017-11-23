package deltas.bytecode.attributes

import core.deltas.grammars.LanguageGrammars
import core.deltas.node.{Node, NodeClass, NodeField}
import core.deltas.{DeltaWithGrammar, Language}
import deltas.bytecode.ByteCodeSkeleton

object UnParsedAttribute extends DeltaWithGrammar {

  class UnParsedAttribute(val node: Node) {
    def nameIndex: Int = node(UnParsedAttribute.Name).asInstanceOf[Int]
    def nameIndex_=(value: Int) = node(UnParsedAttribute.Name) = value

    def data = node(UnParsedAttribute.Data).asInstanceOf[Seq[Byte]]
    def data_=(value: Seq[Byte]) = node(UnParsedAttribute.Data) = value
  }

  def construct(nameIndex: Int, bytes: Seq[Byte]) = new Node(Clazz, Name -> nameIndex, Data -> bytes)

  object Clazz extends NodeClass
  object Name extends NodeField
  object Data extends NodeField

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val grammar = "UnParsed attribute with nameIndex:" ~~> integer.as(Name) asNode Clazz
    find(ByteCodeSkeleton.AttributeGrammar).addOption(grammar)
  }

  override def description: String = "An attribute whose data has not been parsed yet"
}