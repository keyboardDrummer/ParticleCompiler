package miksilo.modularLanguages.deltas.javac.types

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.bigrammar.grammars.Keyword
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.node.{Node, NodeLike, NodeShape}
import miksilo.languageServer.core.language.{Compilation, Language}
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.scopes.objects.Scope
import miksilo.languageServer.core.smarts.types.objects.{PrimitiveType, Type}
import miksilo.modularLanguages.deltas.bytecode.types.{ByteCodeTypeInstance, HasStackTypeDelta, IntTypeDelta}

object BooleanTypeDelta extends ByteCodeTypeInstance
  with HasStackTypeDelta //TODO remove this and change VariablePool accordingly.
{
  val constraintType = PrimitiveType("Boolean")

  override val shape = Shape

  override def getSuperTypes(_type: Node): Seq[Node] = Seq.empty

  override def getByteCodeGrammar(grammars: LanguageGrammars): BiGrammar = {
    import grammars._
    Keyword("Z", reserved = false) ~> value(booleanType)
  }

  override def getStackType(_type: Node, language: Language): Node = IntTypeDelta.intType

  override def getJavaGrammar(grammars: LanguageGrammars) = {
    import grammars._
    "boolean" ~> value(booleanType)
  }

  val booleanType = new Node(Shape)

  object Shape extends NodeShape

  override def description: String = "Defines the boolean type."

  override def getStackSize: Int = IntTypeDelta.getStackSize

  override def getType(compilation: Compilation, builder: ConstraintBuilder, _type: NodeLike, parentScope: Scope): Type = constraintType

  override def constraintName = constraintType.name

  override def fromConstraintType(_type: Type) = booleanType
}
