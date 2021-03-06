package miksilo.modularLanguages.deltas.javac.types

import miksilo.modularLanguages.core.bigrammar.BiGrammar
import miksilo.modularLanguages.core.bigrammar.grammars.Keyword
import miksilo.modularLanguages.core.deltas.{Contract, DeltaWithGrammar}
import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.modularLanguages.core.node.{Node, NodeField, NodeLike, NodeShape}
import miksilo.languageServer.core.language.{Compilation, Language}
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.scopes.objects.Scope
import miksilo.languageServer.core.smarts.types.objects.Type
import miksilo.modularLanguages.deltas.bytecode.types.{HasTypeDelta, TypeSkeleton}

object TypeVariableDelta extends DeltaWithGrammar with HasTypeDelta {

  override def description: String = "Adds references to type variables."

  object Shape extends NodeShape
  object TypeVariableName extends NodeField
  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    transformByteCodeGrammar(grammars)
    transformJavaGrammar(grammars)
  }

  def transformJavaGrammar(grammars: LanguageGrammars): Unit = { //TODO this should not have a grammar that overlaps with UnqualifiedObjectTypeDelta. This and UnqualifiedObjectTypeDelta should depend on a TypeIdentifierDelta.
    import grammars._
    val typeGrammar = find(TypeSkeleton.JavaTypeGrammar)
    val variableGrammar: BiGrammar = identifier.as(TypeVariableName).asNode(Shape)
    typeGrammar.addAlternative(variableGrammar)
  }

  def transformByteCodeGrammar(grammars: LanguageGrammars): Unit = {
    import grammars._
    val byteCodeType = find(TypeSkeleton.ByteCodeTypeGrammar)
    byteCodeType.addAlternative(Keyword("T", false) ~> identifier.as(TypeVariableName) ~< ";" asNode Shape)
  }

  def getTypeVariableName(node: Node): String = {
    node(TypeVariableDelta.TypeVariableName).asInstanceOf[String]
  }

  override def getType(compilation: Compilation, builder: ConstraintBuilder, path: NodeLike, parentScope: Scope): Type = {
    builder.typeVariable() //TODO fix mock implementation
  }

  override def shape: NodeShape = Shape

  override def dependencies: Set[Contract] = Set(TypeSkeleton)
}
