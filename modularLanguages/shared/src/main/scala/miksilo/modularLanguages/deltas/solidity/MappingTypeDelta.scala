package miksilo.modularLanguages.deltas.solidity

import miksilo.modularLanguages.core.deltas.grammars.LanguageGrammars
import miksilo.languageServer.core.language.Compilation
import miksilo.modularLanguages.core.node._
import miksilo.languageServer.core.smarts.ConstraintBuilder
import miksilo.languageServer.core.smarts.scopes.objects.Scope
import miksilo.languageServer.core.smarts.types.objects.{PrimitiveType, Type, TypeApplication}
import miksilo.modularLanguages.deltas.bytecode.types.{TypeInstance, TypeSkeleton}

object MappingTypeDelta extends TypeInstance {
  object Shape extends NodeShape
  object Key extends NodeField
  object Value extends NodeField

  override def description = "Adds the mapping type"

  implicit class MappingType[T <: NodeLike](val node: T) extends NodeWrapper[T] {
    def key = node(Key).asInstanceOf[T]
    def value = node(Value).asInstanceOf[T]
  }

  override def dependencies = Set(ElementaryTypeDelta, TypeSkeleton)

  override def getSuperTypes(_type: Node) = Seq.empty

  override def getJavaGrammar(grammars: LanguageGrammars) = {
    import grammars._
    val elementaryType = find(ElementaryTypeDelta.Shape)
    val typeGrammar = find(TypeSkeleton.JavaTypeGrammar)
    "mapping" ~~ (elementaryType.as(Key) ~~ "=>" ~~ typeGrammar.as(Value)).inParenthesis asNode Shape
  }

  val mappingTypeConstructor = PrimitiveType("mapping")
  override def getType(compilation: Compilation, builder: ConstraintBuilder, path: NodeLike, parentScope: Scope): Type = {
    val mappingType: MappingType[NodeLike] = path
    val keyType = TypeSkeleton.getType(compilation, builder, mappingType.key, parentScope)
    val valueType = TypeSkeleton.getType(compilation, builder, mappingType.value, parentScope)
    TypeApplication(mappingTypeConstructor, Seq(keyType, valueType), path)
  }

  override def shape = Shape
}
