package deltas.expression

import core.deltas.DeltaWithGrammar
import core.deltas.grammars.LanguageGrammars
import core.deltas.path.NodePath
import core.language.node._
import core.language.{Compilation, Language}
import core.smarts.objects.Reference
import core.smarts.scopes.objects.Scope
import core.smarts.types.objects.Type
import core.smarts.{ConstraintBuilder, ResolvesTo}
import deltas.HasNameDelta
import deltas.javac.methods.call.ReferenceExpressionDelta
import deltas.javac.methods.{MethodDelta, VariableInfo}

object VariableDelta extends DeltaWithGrammar with ExpressionInstance with ReferenceExpressionDelta {

  import HasNameDelta._

  implicit class Variable[T <: NodeLike](val node: T) extends HasName[T] {
  }

  def neww(name: String) = new Node(Shape, Name -> name)

  object Shape extends NodeShape

  override def transformGrammars(grammars: LanguageGrammars, state: Language): Unit = {
    import grammars._
    val core = find(ExpressionDelta.LastPrecedenceGrammar)
    val variableGrammar = create(Shape, find(Name) asNode Shape)
    core.addAlternative(variableGrammar)
  }

  override def description: String = "Enables referencing a variable."

  def getVariableInfo(variable: NodePath, compilation: Compilation): VariableInfo = {
    MethodDelta.getMethodCompiler(compilation).getVariables(variable)(variable.name)
  }

  override def shape: NodeShape = Shape

  override def constraints(compilation: Compilation, builder: ConstraintBuilder, variable: NodePath, _type: Type, parentScope: Scope): Unit = {
    val reference = getReference(compilation, builder, variable, parentScope)
    val declaration = builder.declarationVariable(_type)
    builder.add(ResolvesTo(reference, declaration))
  }

  override def getReference(compilation: Compilation, builder: ConstraintBuilder, expression: NodePath, parentScope: Scope): Reference = {
    val variable: Variable[NodePath] = expression
    builder.refer(variable.name, parentScope, Some(expression.getSourceElement(Name)))
  }
}