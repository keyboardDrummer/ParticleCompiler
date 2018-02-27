package deltas.bytecode.types

import core.bigrammar.BiGrammar
import core.bigrammar.grammars.Keyword
import core.deltas.grammars.LanguageGrammars
import core.language.node.{Node, NodeLike, NodeShape}
import core.language.{Compilation, Language}
import core.smarts.ConstraintBuilder
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.{PrimitiveType, Type}

object DoubleTypeDelta extends ByteCodeTypeInstance with HasStackTypeDelta {

  override val shape = DoubleTypeKey

  override def getSuperTypes(_type: Node): Seq[Node] = ???

  override def getByteCodeGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    new Keyword("D",false) ~> value(doubleType)
  }

  override def getStackSize: Int = 2

  override def getJavaGrammar(grammars: LanguageGrammars) = {
    import grammars._
    "double" ~> value(doubleType)
  }

  val doubleType = new Node(shape)

  object DoubleTypeKey extends NodeShape

  override def description: String = "Defines the double type."

  val constraintType = PrimitiveType("Double")
  override def getType(compilation: Compilation, builder: ConstraintBuilder, _type: NodeLike, parentScope: Scope): Type = constraintType
}